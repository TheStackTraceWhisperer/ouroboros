#!/bin/bash

# Product Insight Agent Setup Script

set -e

echo "Setting up Product Insight Agent..."

# Check if Python 3.11+ is available
python_version=$(python3 --version 2>&1 | grep -oP 'Python \K[0-9]+\.[0-9]+')
required_version="3.11"

if [ "$(printf '%s\n' "$required_version" "$python_version" | sort -V | head -n1)" != "$required_version" ]; then
    echo "Error: Python 3.11 or higher required. Found: $python_version"
    exit 1
fi

echo "✓ Python version check passed: $python_version"

# Create virtual environment
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

echo "✓ Virtual environment ready"

# Activate virtual environment
source venv/bin/activate

# Upgrade pip
pip install --upgrade pip

# Install dependencies
echo "Installing dependencies..."
pip install -r requirements.txt

echo "✓ Dependencies installed"

# Create data directories
mkdir -p data reports logs

echo "✓ Directories created"

# Copy environment file if it doesn't exist
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "✓ Environment file created from template"
    echo ""
    echo "IMPORTANT: Please edit .env file with your API credentials:"
    echo "  - Reddit API credentials (REDDIT_CLIENT_ID, REDDIT_CLIENT_SECRET)"
    echo "  - LLM API key (ANTHROPIC_API_KEY or GOOGLE_API_KEY)"
    echo "  - Database URL if using external database"
    echo ""
else
    echo "✓ Environment file already exists"
fi

echo ""
echo "Setup complete! 🎉"
echo ""
echo "Next steps:"
echo "1. Edit .env file with your API credentials"
echo "2. Test connections: python -m src.main test"
echo "3. Run pipeline: python -m src.main pipeline"
echo ""
echo "For more commands, run: python -m src.main --help"