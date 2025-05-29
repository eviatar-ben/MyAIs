# Gemini Python API - Simple Demo

A clean, easy-to-use Python wrapper for Google's Gemini AI models.

## 📁 Files

- **`gemini.py`** - The main Gemini API wrapper (black box implementation)
- **`main.py`** - Simple demo showing basic usage
- **`examples.py`** - Advanced usage examples

## 🚀 Quick Start

### 1. Basic Usage

```python
from gemini import init_model

# Initialize with model selection menu
gemini = init_model()

# Ask a question
response = gemini.ask("What is artificial intelligence?")
print(response)
```

### 2. Use Specific Model

```python
from gemini import init_model

# Use a specific model directly
gemini = init_model("gemini-1.5-flash")
response = gemini.ask("Explain Python in simple terms")
print(response)
```

### 3. Multiple Questions

```python
from gemini import init_model

gemini = init_model("gemini-2.0-flash")

# Ask multiple questions in the same session
response1 = gemini.ask("What is Python?")
response2 = gemini.ask("How do I create a list?")
response3 = gemini.ask("What's the difference between list and tuple?")
```

## 🤖 Available Models

1. **gemini-1.5-flash** - Fast and versatile (recommended for beginners)
2. **gemini-1.5-pro** - Complex reasoning tasks (more powerful)
3. **gemini-2.0-flash** - Newest multimodal, fastest
4. **gemini-2.0-flash-lite** - Most cost-efficient
5. **gemini-2.5-flash-preview-05-20** - Best price-performance with thinking capabilities
6. **gemini-2.5-pro-preview-05-06** - Most powerful thinking model (advanced reasoning)

## 📖 API Reference

### `init_model(model_name=None)`
Initialize a Gemini model.
- **model_name** (optional): Specific model to use. If None, shows selection menu.
- **Returns**: Ready-to-use Gemini instance

### `gemini.ask(question, short_answer=True)`
Ask Gemini a question.
- **question**: The question to ask
- **short_answer**: Whether to request a concise answer (default: True)
- **Returns**: Gemini's response as string

### `gemini.get_model_name()`
Get the current model name.

### `gemini.get_available_models()`
Get dictionary of available models with descriptions.

## 🏃‍♂️ Running the Examples

```bash
# Basic demo
python main.py

# Advanced examples
python examples.py

# Legacy demo
python gemini_demo.py
```

## 🔧 Requirements

- Python 3.7+
- `google-generativeai`
- `google-auth` (for using service account JSON files)
- API key (same as Java version - provided by hackathon organizers)
- **Service Account JSON File**: You need a Google Cloud service account JSON file for authentication. 
    - **Important**: After obtaining your JSON file, you **MUST** update the `_SERVICE_ACCOUNT_FILE_PATH` variable in the `gemini.py` file to point to the correct path of your JSON key file. For example:
      ```python
      # Inside gemini.py
      ...
      class Gemini:
          _SERVICE_ACCOUNT_FILE_PATH = "path/to/your/service-account-file.json" 
      ...
      ```

## 💡 Tips for Students

1. **Start Simple**: Use `main.py` to understand the basics
2. **Explore Models**: Try different models for different tasks
3. **Ask Follow-ups**: Use the same `gemini` instance for related questions
4. **Handle Errors**: Wrap your code in try-except blocks
5. **Read Examples**: Check `examples.py` for advanced usage patterns


**Happy Hacking! 🚀** 