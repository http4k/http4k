package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource.Content.Text
import org.http4k.ai.mcp.model.Resource.Static
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.extension.McpApps
import org.http4k.core.Uri
import org.http4k.routing.bind

object OrderFormUi {
    val uri = Uri.of("ui://order-form")

    val resource = Static(
        uri = uri,
        name = ResourceName.of("Order Form"),
        description = "Interactive order form",
        mimeType = McpApps.MIME_TYPE
    ) bind { ResourceResponse(Text(ORDER_FORM_HTML, it.uri, McpApps.MIME_TYPE)) }
}

private val ORDER_FORM_HTML = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Order Form</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: system-ui, -apple-system, sans-serif;
            margin: 0;
            padding: 16px;
            background: var(--bg, #ffffff);
            color: var(--fg, #171717);
        }
        h1 { margin-top: 0; font-size: 1.25rem; }
        .form-group { margin-bottom: 12px; }
        label { display: block; margin-bottom: 4px; font-weight: 500; }
        input, select {
            width: 100%;
            padding: 8px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 14px;
        }
        button {
            background: #0066cc;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
        }
        button:hover { background: #0055aa; }
        button:disabled { background: #ccc; cursor: not-allowed; }
        #result {
            margin-top: 16px;
            padding: 12px;
            background: #f5f5f5;
            border-radius: 4px;
            display: none;
        }
        #result.visible { display: block; }
        .error { color: #cc0000; }
    </style>
</head>
<body>
    <h1>Order Form</h1>

    <form id="order-form">
        <div class="form-group">
            <label for="product">Product</label>
            <input type="text" id="product" name="product" placeholder="Enter product name" required>
        </div>

        <div class="form-group">
            <label for="quantity">Quantity</label>
            <input type="number" id="quantity" name="quantity" min="1" value="1" required>
        </div>

        <button type="submit" id="submit-btn">Submit Order</button>
    </form>

    <div id="result"></div>

    <script>
        // MCP App Communication Layer
        let requestId = 1;
        const pendingRequests = new Map();

        function sendRequest(method, params = {}) {
            const id = requestId++;
            return new Promise((resolve, reject) => {
                pendingRequests.set(id, { resolve, reject });
                window.parent.postMessage({ jsonrpc: "2.0", id, method, params }, '*');
                setTimeout(() => {
                    if (pendingRequests.has(id)) {
                        pendingRequests.delete(id);
                        reject(new Error('Request timed out'));
                    }
                }, 30000);
            });
        }

        window.addEventListener('message', (event) => {
            const data = event.data;
            if (!data || typeof data !== 'object') return;

            if (data.id !== undefined && pendingRequests.has(data.id)) {
                const { resolve, reject } = pendingRequests.get(data.id);
                pendingRequests.delete(data.id);
                if (data.error) {
                    reject(new Error(data.error.message || 'Unknown error'));
                } else {
                    resolve(data.result);
                }
            }

            // Handle notifications (tool input)
            if (data.method === 'ui/notifications/tool-input' && data.params) {
                const args = data.params.arguments || {};
                if (args.product) document.getElementById('product').value = args.product;
                if (args.quantity) document.getElementById('quantity').value = args.quantity;
            }
        });

        // Initialize connection
        async function init() {
            try {
                const result = await sendRequest("ui/initialize", {
                    protocolVersion: "2025-06-18",
                    clientInfo: { name: "Order Form", version: "1.0.0" },
                    capabilities: {}
                });
                console.log('Connected to host:', result);

                // Apply theme
                if (result.hostContext?.theme === 'dark') {
                    document.body.style.setProperty('--bg', '#1a1a1a');
                    document.body.style.setProperty('--fg', '#fafafa');
                }
            } catch (e) {
                console.error('Failed to initialize:', e);
            }
        }

        // Form handling
        const form = document.getElementById('order-form');
        const submitBtn = document.getElementById('submit-btn');
        const resultDiv = document.getElementById('result');

        function showResult(message, isError = false) {
            resultDiv.textContent = message;
            resultDiv.className = isError ? 'visible error' : 'visible';
        }

        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            const product = document.getElementById('product').value;
            const quantity = parseInt(document.getElementById('quantity').value);

            submitBtn.disabled = true;
            submitBtn.textContent = 'Submitting...';

            try {
                const result = await sendRequest("tools/call", {
                    name: "submit_order",
                    arguments: { product, quantity }
                });

                const text = result.content?.find(c => c.type === 'text')?.text
                    || 'Order submitted!';
                showResult(text);
            } catch (error) {
                showResult('Error: ' + error.message, true);
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Submit Order';
            }
        });

        init();
    </script>
</body>
</html>
""".trimIndent()
