from ai_engine import ai_engine
import json

def test_ask():
    print("Testing 'ask'...")
    response = ai_engine.ask("Hello, are you working?")
    if response:
        print(f"✅ Ask Success: {response[:50]}...")
    else:
        print("❌ Ask Failed")

def test_quiz():
    print("\nTesting 'generate_quiz'...")
    quiz = ai_engine.generate_quiz("Cardiology", "Medical Student", "Heart Failure")
    if quiz and "questions" in quiz:
        print(f"✅ Quiz Success: {quiz['quiz_title']}")
        print(f"Questions generated: {len(quiz['questions'])}")
    else:
        print("❌ Quiz Failed")

def test_recommendations():
    print("\nTesting 'get_recommendations'...")
    user_info = {
        "username": "TestUser",
        "ai_persona": "Dr. House",
        "semester_status": "Year 3"
    }
    active_units = ["Anatomy", "Physiology"]
    rec = ai_engine.get_recommendations(user_info, [], active_units)
    if rec:
        print(f"✅ Recommendation Success: {rec[:100]}...")
    else:
        print("❌ Recommendation Failed")

if __name__ == "__main__":
    print("🚀 Starting AI Engine verification tests...\n")
    test_ask()
    test_quiz()
    test_recommendations()
    print("\n🏁 Verification complete.")
