window.onload = function() {
    document.title = "%%PAGE_TITLE%%";
    SwaggerUIBundle({
        url: "%%DESCRIPTION_ROUTE%%",
        dom_id: "#%%DOM_ID%%",
        deepLinking: %%DEEP_LINKING%%,
        displayOperationId: %%DISPLAY_OPERATION_ID%%,
        displayRequestDuration: %%DISPLAY_REQUEST_DURATION%%,
        requestSnippetsEnabled: %%REQUEST_SNIPPETS_ENABLED%%,
        persistAuthorization: %%PERSIST_AUTHORIZATION%%,
        presets: [%%PRESETS%%],
        layout: "%%LAYOUT%%",
        queryConfigEnabled: %%QUERY_CONFIG_ENABLED%%,
        tryItOutEnabled: %%TRY_IT_OUT_ENABLED%%,
        oauth2RedirectUrl: %%OAUTH2_REDIRECT_URL%%,
        withCredentials: %%WITH_CREDENTIALS%%
    })
}
