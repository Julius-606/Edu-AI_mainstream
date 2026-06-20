# Custom Authentication Migration Walkthrough

I have replaced Firebase with a fully custom, backend-driven authentication system. This gives you full control over user data and security.

## 🏗️ The New Architecture

### 1. Backend (The Vault)
*   **Secure Storage**: Passwords are now hashed using **Bcrypt** before being stored in your database. Even if your database is leaked, the passwords remain unreadable.
*   **JWT Tokens**: When a user logs in, the backend issues a **JSON Web Token (JWT)**. This token is used to authorize all future requests.
*   **Web Signup**: I created a clean, responsive signup page at `/signup` using Tailwind CSS. This allows you to update the signup process instantly without pushing a new app update.

### 2. Android App (The Keyring)
*   **Encrypted Storage**: The app uses `EncryptedSharedPreferences` to store the JWT. This uses hardware-backed encryption to keep the token safe from other apps on the device.
*   **Auto-Authorization**: Every request sent to the backend now carries two "keys":
    1.  `X-Internal-Api-Key`: Proves the request is from your app.
    2.  `Authorization: Bearer <token>`: Proves who the user is.
*   **Browser-to-App Flow**: The "Sign Up" button launches the user's browser to the secure backend page. Once finished, they return to the app to log in.

---

## 🚀 Final Steps to Go Live

### Step 1: Update Backend Environment
Install the new security libraries on your server (Huggingface/VPS):
```bash
pip install passlib[bcrypt] python-jose[cryptography] jinja2 python-multipart
```

### Step 2: Set Your Secret Keys
Set these environment variables on your server:
1.  `INTERNAL_API_KEY`: The same key you have in `local.properties`.
2.  `JWT_SECRET_KEY`: A long, random string (e.g., `openssl rand -hex 32`). **Keep this very secret.**

### Step 3: Run the App
1.  **Sync Gradle**: Click the elephant icon 🐘 in Android Studio to pull in the `androidx.security` library.
2.  **Launch**: The app should build immediately (no `google-services.json` required!).
3.  **Test**: Click "Sign Up", create an account in the browser, then return to the app to log in.

> [!TIP]
> Since we've updated the database schema, you may need to clear your local app data (Settings > Apps > Edu-AI > Clear Data) to ensure the new `userId` logic works smoothly.
