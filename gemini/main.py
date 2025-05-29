from gemini import init_model


def main():
    """
    Simple demo showing how to use the Gemini class
    The Gemini implementation is hidden - just use it as a black box!
    """

    print("🤖 Gemini Python Demo")
    print("=" * 50)

    try:
        # Initialize Gemini model (will show model selection menu)
        gemini = init_model()

        print("=" * 50)

        # Ask a question
        question = input("🤔 Question: ")

        print("\n🤖 Gemini's Response:")
        print("-" * 30)

        # Get response from Gemini - simple as that!
        response = gemini.ask(question)
        print(response)

        print("\n" + "=" * 50)
        print(f"💡 Used model: {gemini.get_model_name()}")

    except KeyboardInterrupt:
        print("\n👋 Goodbye!")
    except Exception as e:
        print(f"❌ Error occurred: {e}")
        print("💡 Make sure you have the required dependencies and service account file")


if __name__ == "__main__":
    main()
