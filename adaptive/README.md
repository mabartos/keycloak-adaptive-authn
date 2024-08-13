# Adaptive Authentication module

This module contains components for providing Adaptive authentication capabilities.

It leverages risk-based authentication, and communicate with OpenAI ChatGPT NPL engine.

## Start the distribution with the extension

You can simply execute this command in this module and a Keycloak distribution with this extension will start:
```shell
../mvnw exec:exec@start-server
```

## Integration with OpenAI
In order to use the default OpenAI engine for risk scoring, create `.env` file in the working directory, or set following environment variables:

- `OPEN_AI_API_KEY` - OpenAI API key
- `OPEN_AI_API_ORGANIZATION` - OpenAI organization ID
- `OPEN_AI_API_PROJECT` - OpenAI project ID
- `OPEN_AI_API_URL`(optional) - OpenAI URL (default 'https://api.openai.com/v1/chat/completions')