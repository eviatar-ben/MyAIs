from gemini import Gemini, init_model


def basic_example():
    """Example 1: Basic usage with model selection"""
    print("=== Example 1: Basic Usage ===")

    # Let user choose model interactively
    gemini = init_model()

    response = gemini.ask("What is artificial intelligence?")
    print(f"Response: {response}")
    print()


def specific_model_example():
    """Example 2: Using a specific model"""
    print("=== Example 2: Specific Model ===")

    # Use a specific model directly
    gemini = init_model("gemini-2.0-flash")

    response = gemini.ask("Explain quantum computing in simple terms")
    print(f"Response: {response}")
    print()


def long_answer_example():
    """Example 3: Getting detailed answers"""
    print("=== Example 3: Detailed Answer ===")

    gemini = init_model("gemini-1.5-pro")

    # Request a detailed answer
    response = gemini.ask("How does machine learning work?", short_answer=False)
    print(f"Detailed response: {response}")
    print()


def multiple_questions_example():
    """Example 4: Multiple questions in same session"""
    print("=== Example 4: Multiple Questions ===")

    gemini = init_model("gemini-2.0-flash")

    questions = [
        "What is Python?",
        "How do I create a list in Python?",
        "What is the difference between a list and a tuple?"
    ]

    for i, question in enumerate(questions, 1):
        print(f"Q{i}: {question}")
        response = gemini.ask(question)
        print(f"A{i}: {response}\n")


def model_info_example():
    """Example 6: Getting model information"""
    print("=== Example 6: Model Information ===")

    gemini = init_model("gemini-2.0-flash-lite")

    print(f"Current model: {gemini.get_model_name()}")
    print("Available models:")

    models = gemini.get_available_models()
    for model, description in models.items():
        print(f"  - {model}: {description}")

    print()


def main():
    """Run all examples"""
    print("🚀 Gemini Python Examples")
    print("=" * 60)

    examples = [
        basic_example,
        specific_model_example,
        long_answer_example,
        multiple_questions_example,
        model_info_example
    ]

    for example in examples:
        try:
            example()
        except KeyboardInterrupt:
            print("\n👋 Goodbye!")
            break
        except Exception as e:
            print(f"❌ Error in {example.__name__}: {e}")
            print()


if __name__ == "__main__":
    main()
