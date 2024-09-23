# Adaptive Authentication module

This module contains components for providing Adaptive authentication capabilities.

It leverages risk-based authentication, and communicate with OpenAI ChatGPT NPL engine.

## Start the distribution with the extension

You can simply execute this command in this module and a Keycloak distribution with this extension will start:
```shell
../mvnw exec:exec@start-server
```

## Show example flow

In order to see the execution of the authentication flow from the example realm `adaptive`, just access the url `http://localhost:8080/admin/adaptive/console/`.

## Integration with OpenAI
In order to use the default OpenAI engine for risk scoring, create `.env` file in the working directory, or set following environment variables:

- `OPEN_AI_API_KEY` - OpenAI API key
- `OPEN_AI_API_ORGANIZATION` - OpenAI organization ID
- `OPEN_AI_API_PROJECT` - OpenAI project ID
- `OPEN_AI_API_URL`(optional) - OpenAI URL (default 'https://api.openai.com/v1/chat/completions')
- `OPEN_AI_API_MODEL`(optional) - OpenAI Model type (default `gpt-3.5-turbo`)

## Integration with IBM Granite

In order to use the IBM Granite NLP engine for risk scoring, create `.env` file in the working directory, or set
following environment variables:

- `GRANITE_API_KEY` - Granite API key
- `GRANITE_API_URL` - Granite API URL
- `GRANITE_API_MODEL`(optional) - Granite API Model (default `granite-8b-code-instruct-128k`)

**WARNING**: It seems the IMB Granite is slower than OpenAI ChatGPT, so the timeout for risk evaluations needs to be
increased for now.