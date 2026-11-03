const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");

function loadLocalSettings() {
    const settingsFile = path.join(__dirname, ".env");
    if (!fs.existsSync(settingsFile)) return;
    for (const line of fs.readFileSync(settingsFile, "utf8").split(/\r?\n/)) {
        const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/);
        if (match && !Object.hasOwn(process.env, match[1])) {
            process.env[match[1]] = match[2].replace(/^(['"])(.*)\1$/, "$2");
        }
    }
}

loadLocalSettings();

const port = Number(process.env.PORT || 3000);
const dataDirectory = path.join(__dirname, "data");
const paymentsFile = path.join(dataDirectory, "payments.json");
const darajaEnvironment = (process.env.DARAJA_ENV || "sandbox").toLowerCase();
if (darajaEnvironment !== "sandbox" && darajaEnvironment !== "production") {
    throw new Error("DARAJA_ENV must be either sandbox or production");
}
const darajaBaseUrl = darajaEnvironment === "production"
    ? "https://api.safaricom.co.ke"
    : "https://sandbox.safaricom.co.ke";
const maxAmount = 150000;
const requestLimits = new Map();
const phoneRequestLimits = new Map();

function requiredSetting(name) {
    const value = process.env[name];
    if (!value) {
        throw new Error(`Missing required environment setting: ${name}`);
    }
    return value;
}

function loadPayments() {
    try {
        return JSON.parse(fs.readFileSync(paymentsFile, "utf8"));
    } catch (error) {
        if (error.code === "ENOENT") {
            return {};
        }
        throw error;
    }
}

let payments = loadPayments();

function savePayments() {
    fs.mkdirSync(dataDirectory, { recursive: true });
    const temporaryFile = `${paymentsFile}.tmp`;
    fs.writeFileSync(temporaryFile, JSON.stringify(payments, null, 2), { mode: 0o600 });
    fs.renameSync(temporaryFile, paymentsFile);
}

function sendJson(response, statusCode, body) {
    response.writeHead(statusCode, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify(body));
}

function isRateLimited(request, phoneNumber) {
    const address = request.socket.remoteAddress || "unknown";
    const now = Date.now();
    const recent = (requestLimits.get(address) || []).filter((time) => now - time < 60000);
    recent.push(now);
    requestLimits.set(address, recent);
    const phoneRecent = (phoneRequestLimits.get(phoneNumber) || [])
        .filter((time) => now - time < 600000);
    phoneRecent.push(now);
    phoneRequestLimits.set(phoneNumber, phoneRecent);
    return recent.length > 10 || phoneRecent.length > 3;
}

function readJson(request) {
    return new Promise((resolve, reject) => {
        let body = "";
        request.on("data", (chunk) => {
            body += chunk;
            if (body.length > 8192) {
                reject(new Error("Request body too large"));
                request.destroy();
            }
        });
        request.on("end", () => {
            try {
                resolve(JSON.parse(body || "{}"));
            } catch (error) {
                reject(new Error("Invalid JSON body"));
            }
        });
        request.on("error", reject);
    });
}

function normalizePhone(input) {
    let phone = String(input || "").replace(/[\s-]/g, "");
    if (phone.startsWith("+")) phone = phone.slice(1);
    if (phone.startsWith("0")) phone = `254${phone.slice(1)}`;
    return /^254[17]\d{8}$/.test(phone) ? phone : null;
}

function timestamp() {
    const parts = Object.fromEntries(new Intl.DateTimeFormat("en-GB", {
        timeZone: "Africa/Nairobi",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hourCycle: "h23"
    }).formatToParts(new Date()).map((part) => [part.type, part.value]));
    return `${parts.year}${parts.month}${parts.day}${parts.hour}${parts.minute}${parts.second}`;
}

async function darajaRequest(endpoint, payload) {
    const consumerKey = requiredSetting("DARAJA_CONSUMER_KEY");
    const consumerSecret = requiredSetting("DARAJA_CONSUMER_SECRET");
    const credentials = Buffer.from(`${consumerKey}:${consumerSecret}`).toString("base64");
    const tokenResponse = await fetch(
        `${darajaBaseUrl}/oauth/v1/generate?grant_type=client_credentials`,
        { headers: { Authorization: `Basic ${credentials}` } }
    );
    const tokenBody = await tokenResponse.json();
    if (!tokenResponse.ok || !tokenBody.access_token) {
        throw new Error("Could not get a Daraja access token");
    }

    const apiResponse = await fetch(`${darajaBaseUrl}${endpoint}`, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${tokenBody.access_token}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    });
    const responseBody = await apiResponse.json();
    if (!apiResponse.ok) {
        throw new Error(responseBody.errorMessage || "Daraja request failed");
    }
    return responseBody;
}

function darajaPassword(shortcode, passkey, requestTimestamp) {
    return Buffer.from(`${shortcode}${passkey}${requestTimestamp}`).toString("base64");
}

async function sendStkPush({ phoneNumber, amount, accountReference }) {
    const shortcode = requiredSetting("DARAJA_SHORTCODE");
    const passkey = requiredSetting("DARAJA_PASSKEY");
    const callbackUrl = requiredSetting("DARAJA_CALLBACK_URL");
    const requestTimestamp = timestamp();
    return darajaRequest("/mpesa/stkpush/v1/processrequest", {
        BusinessShortCode: shortcode,
        Password: darajaPassword(shortcode, passkey, requestTimestamp),
        Timestamp: requestTimestamp,
        TransactionType: process.env.DARAJA_TRANSACTION_TYPE || "CustomerPayBillOnline",
        Amount: amount,
        PartyA: phoneNumber,
        PartyB: shortcode,
        PhoneNumber: phoneNumber,
        CallBackURL: callbackUrl,
        AccountReference: accountReference,
        TransactionDesc: "University registration fees"
    });
}

async function queryStkPush(checkoutRequestId) {
    const shortcode = requiredSetting("DARAJA_SHORTCODE");
    const requestTimestamp = timestamp();
    return darajaRequest("/mpesa/stkpushquery/v1/query", {
        BusinessShortCode: shortcode,
        Password: darajaPassword(shortcode, requiredSetting("DARAJA_PASSKEY"), requestTimestamp),
        Timestamp: requestTimestamp,
        CheckoutRequestID: checkoutRequestId
    });
}

function callbackMetadata(callback) {
    const items = callback?.CallbackMetadata?.Item || [];
    return Object.fromEntries(items.map((item) => [item.Name, item.Value]));
}

async function processCallback(payload) {
    const callback = payload?.Body?.stkCallback;
    const checkoutRequestId = callback?.CheckoutRequestID;
    const payment = payments[checkoutRequestId];
    if (!payment || payment.status !== "PENDING") return;

    const resultCode = Number(callback.ResultCode);
    if (resultCode !== 0) {
        payment.status = "FAILED";
        payment.failureReason = String(callback.ResultDesc || "Payment was not completed").slice(0, 160);
        savePayments();
        return;
    }

    const metadata = callbackMetadata(callback);
    const callbackAmount = Number(metadata.Amount);
    const callbackPhone = String(metadata.PhoneNumber || "");
    const receiptNumber = String(metadata.MpesaReceiptNumber || "");
    if (callbackAmount !== payment.amount || callbackPhone !== payment.phoneNumber || !receiptNumber) {
        payment.status = "FAILED";
        payment.failureReason = "Payment callback did not match the requested amount and phone";
        savePayments();
        return;
    }

    const queryResult = await queryStkPush(checkoutRequestId);
    if (Number(queryResult.ResultCode) !== 0) {
        payment.status = "FAILED";
        payment.failureReason = String(queryResult.ResultDesc || "Safaricom did not confirm payment").slice(0, 160);
        savePayments();
        return;
    }

    payment.status = "PAID";
    payment.receiptNumber = receiptNumber;
    payment.paidAt = new Date().toISOString();
    savePayments();
}

const server = http.createServer(async (request, response) => {
    const requestUrl = new URL(request.url, `http://${request.headers.host || "localhost"}`);

    if (request.method === "GET" && requestUrl.pathname === "/health") {
        return sendJson(response, 200, { status: "ok" });
    }

    if (request.method === "POST" && requestUrl.pathname === "/api/payments/callback") {
        try {
            const body = await readJson(request);
            sendJson(response, 200, { ResultCode: 0, ResultDesc: "Accepted" });
            processCallback(body).catch((error) => console.error("Callback verification failed:", error.message));
        } catch (error) {
            sendJson(response, 400, { error: error.message });
        }
        return;
    }

    if (request.method === "POST" && requestUrl.pathname === "/api/payments/stk-push") {
        try {
            const body = await readJson(request);
            const phoneNumber = normalizePhone(body.phoneNumber);
            const amount = Number(body.amount);
            const accountReference = String(body.accountReference || "").trim();
            if (!phoneNumber || !Number.isInteger(amount) || amount < 1 || amount > maxAmount
                    || !/^[A-Za-z0-9-]{1,40}$/.test(accountReference)) {
                return sendJson(response, 400, { error: "Enter a valid phone, amount, and student reference." });
            }
            if (isRateLimited(request, phoneNumber)) {
                return sendJson(response, 429, { error: "Too many prompts. Wait a few minutes before trying again." });
            }

            const stkResponse = await sendStkPush({ phoneNumber, amount, accountReference });
            if (String(stkResponse.ResponseCode) !== "0" || !stkResponse.CheckoutRequestID) {
                return sendJson(response, 502, {
                    error: stkResponse.ResponseDescription || "Safaricom could not start the payment."
                });
            }

            payments[stkResponse.CheckoutRequestID] = {
                studentReference: accountReference,
                phoneNumber,
                amount,
                status: "PENDING",
                createdAt: new Date().toISOString()
            };
            savePayments();
            return sendJson(response, 200, {
                checkoutRequestId: stkResponse.CheckoutRequestID,
                customerMessage: stkResponse.CustomerMessage || "Approve the M-PESA prompt on your phone."
            });
        } catch (error) {
            console.error("Payment request failed:", error.message);
            return sendJson(response, 502, { error: "Could not start payment. Check the server setup and try again." });
        }
    }

    const statusMatch = request.method === "GET"
        && requestUrl.pathname.match(/^\/api\/payments\/([^/]+)$/);
    if (statusMatch) {
        const checkoutRequestId = decodeURIComponent(statusMatch[1]);
        const payment = payments[checkoutRequestId];
        if (!payment) return sendJson(response, 404, { error: "Payment not found." });
        return sendJson(response, 200, {
            checkoutRequestId,
            status: payment.status,
            receiptNumber: payment.receiptNumber || null,
            failureReason: payment.failureReason || null
        });
    }

    return sendJson(response, 404, { error: "Not found." });
});

server.listen(port, "0.0.0.0", () => {
    console.log(`M-PESA payment server listening on port ${port}`);
});