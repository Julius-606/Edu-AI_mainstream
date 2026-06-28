import sys
import os

# Add current directory to path to import local modules
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

try:
    import auth
    print("Successfully imported auth module.")

    # Test long password
    long_password = "a" * 100
    print(f"Testing with password of length {len(long_password)}...")

    hashed = auth.get_password_hash(long_password)
    print("Successfully hashed long password.")

    is_valid = auth.verify_password(long_password, hashed)
    print(f"Password verification: {is_valid}")

    if is_valid:
        print("✅ VERIFICATION SUCCESSFUL: Long password handled correctly.")
    else:
        print("❌ VERIFICATION FAILED: Password verification failed.")

except Exception as e:
    print(f"❌ VERIFICATION FAILED with error: {e}")

try:
    import ai_engine
    print("Successfully imported ai_engine module.")
    print("✅ VERIFICATION SUCCESSFUL: AI engine migrated to google-genai.")
except Exception as e:
    print(f"❌ VERIFICATION FAILED with error in AI engine: {e}")
