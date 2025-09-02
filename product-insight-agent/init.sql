-- Initialize database schema for Product Insight Agent

-- Create the product_insights schema
CREATE SCHEMA IF NOT EXISTS product_insights;

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON SCHEMA product_insights TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA product_insights TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA product_insights TO postgres;