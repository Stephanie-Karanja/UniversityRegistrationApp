# Real M-PESA Payment Setup

The Android app sends payment requests to the small Node.js server in `backend/`. That server calls Safaricom Daraja and handles the payment result. The app must never contain the Daraja consumer secret or passkey.

## 1. Configure the server

Install Node.js 20 or newer. Copy `backend/.env.example` to `backend/.env` and fill in the production values supplied by Safaricom:

- `DARAJA_CONSUMER_KEY`
- `DARAJA_CONSUMER_SECRET`
- `DARAJA_SHORTCODE`
- `DARAJA_PASSKEY`
- `DARAJA_CALLBACK_URL`

Never commit `backend/.env` or send its contents in chat. It is ignored by Git.

The callback must be a public HTTPS address ending in `/api/payments/callback`. Deploy the server to a host with HTTPS and persistent storage for `backend/data/payments.json`, then set the callback URL in `backend/.env` to that deployed address. Start the server from the backend folder with `node server.js`. It listens on port 3000 by default; the hosting provider may set `PORT`.

For testing before Safaricom approves a production Till or Paybill, set `DARAJA_ENV=sandbox` and use the Daraja sandbox app credentials, shortcode, passkey, and test phone number. The backend defaults to sandbox when `DARAJA_ENV` is omitted. Sandbox transactions are simulated and do not move real money; a real M-PESA prompt to an ordinary phone requires production access. Only switch to `DARAJA_ENV=production` after the institution's merchant account is approved and its production Daraja details are configured.

## 2. Point Android to the server

In `app/src/main/java/com/example/universityregistration/utils/MpesaPaybill.java`, replace `https://replace-with-your-deployed-server` with the server's base address, for example `https://payments.example.ac.ke` (no trailing slash). This is the only URL the Android app needs.

## 3. Test the payment

Open the app, enter the student's reference, Kenyan M-PESA phone number, and amount, then tap **Send M-PESA prompt**. Approve the prompt on the phone. The app stays pending until the server receives the Safaricom callback, checks the amount and phone, and confirms the result using Daraja's STK Push Query API. Only then does the app report success and show the M-PESA receipt number.

## Before accepting public payments

This is a small assignment backend, not a finished production payment platform. The project has no student login or fee database yet. Before exposing it to the public, add authenticated student accounts, verify the expected fee on the server instead of trusting the amount sent by the app, and use a managed database with backups rather than the included JSON file. The built-in request limits are only basic protection. Production Daraja transactions move real money; first test with the Safaricom setup and account that your institution authorizes.