# Product Insight Agent

A Python-based service for ingesting and analyzing user feedback from external sources.

## Overview

The Product Insight Agent is designed to:
1. Ingest unstructured user feedback from external sources (starting with Reddit)
2. Process and normalize data into structured format
3. Analyze feedback using LLM for sentiment and topic extraction
4. Generate daily summary reports
5. Store data for further analysis

## Features

- **Data Ingestion**: Reddit API integration using PRAW
- **Data Processing**: Normalize raw data into FeedbackItem structure
- **LLM Analysis**: Sentiment analysis and topic extraction
- **Reporting**: Daily automated summary generation
- **Storage**: Database integration for structured data storage
- **Containerized**: Docker support for easy deployment
- **Scheduled**: Automated execution capability

## Architecture

This agent operates independently from the main Ouroboros Spring Boot application, allowing for parallel development and scaling.

## Getting Started

### Prerequisites
- Python 3.11+
- Docker (optional)
- Reddit API credentials
- LLM API access (Claude, Gemini, etc.)

### Installation

```bash
cd product-insight-agent
pip install -r requirements.txt
```

### Configuration

Copy `.env.example` to `.env` and configure your API credentials:

```bash
cp .env.example .env
# Edit .env with your credentials
```

### Running

```bash
# Run data ingestion
python -m src.main ingest

# Run analysis
python -m src.main analyze

# Run reporting
python -m src.main report

# Run full pipeline
python -m src.main pipeline
```

### Docker

```bash
# Build image
docker build -t product-insight-agent .

# Run container
docker run -d --env-file .env product-insight-agent
```