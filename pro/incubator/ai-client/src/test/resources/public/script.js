document.addEventListener('DOMContentLoaded', function() {
    const chatContainer = document.getElementById('chat-container');
    const messageForm = document.getElementById('message-form');
    const messageInput = document.getElementById('message-input');
    const statusElement = document.getElementById('status');

    // Initial state check
    checkState();

    // Setup form submission
    messageForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const message = messageInput.value.trim();
        if (message) {
            sendMessage(message);
            messageInput.value = '';
        }
    });

    // Check current state
    async function checkState() {
        try {
            setStatus('Checking state...');
            const response = await fetch('/api/state');
            const data = await response.json();
            handleStateChange(data);
        } catch (error) {
            setStatus('Error: ' + error.message);
        }
    }

    // Send a message
    async function sendMessage(message) {
        // Add user message to chat
        addMessage(message, 'user');

        try {
            setStatus('Sending message...');
            const response = await fetch('/api/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ message })
            });

            const data = await response.json();
            handleStateChange(data);
        } catch (error) {
            setStatus('Error: ' + error.message);
        }
    }

    // Approve a tool
    async function approveTool() {
        try {
            setStatus('Approving tool...');
            const response = await fetch('/api/tool/approve', {
                method: 'POST'
            });

            const data = await response.json();
            handleStateChange(data);
        } catch (error) {
            setStatus('Error: ' + error.message);
        }
    }

    // Reject a tool
    async function rejectTool() {
        try {
            setStatus('Rejecting tool...');
            const response = await fetch('/api/tool/reject', {
                method: 'POST'
            });

            const data = await response.json();
            handleStateChange(data);
        } catch (error) {
            setStatus('Error: ' + error.message);
        }
    }

    // Handle state changes from the API
    function handleStateChange(data) {
        const state = data.state;
        const stateType = state.type || Object.keys(state)[0];

        setStatus('Current state: ' + stateType);

        if (stateType === 'AwaitingApproval') {
            // Tool approval needed - show a modal
            showToolApprovalModal(data.message || 'Tool requires approval');
        } else if (stateType === 'Responding' && data.message) {
            // Show AI response
            addMessage(data.message, 'ai');
        }
    }

    // Add a message to the chat
    function addMessage(text, sender) {
        const messageElement = document.createElement('div');
        messageElement.className = `message ${sender}-message`;
        messageElement.textContent = text;
        chatContainer.appendChild(messageElement);
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    // Show modal for tool approval
    function showToolApprovalModal(toolDescription) {
        // Create modal elements
        const modalOverlay = document.createElement('div');
        modalOverlay.className = 'modal-overlay';

        const modal = document.createElement('div');
        modal.className = 'modal';

        const modalHeader = document.createElement('div');
        modalHeader.className = 'modal-header';
        modalHeader.textContent = 'Tool Approval Required';

        const modalContent = document.createElement('div');
        modalContent.className = 'modal-content';
        modalContent.textContent = toolDescription;

        const modalButtons = document.createElement('div');
        modalButtons.className = 'modal-buttons';

        const approveBtn = document.createElement('button');
        approveBtn.className = 'approve-btn';
        approveBtn.textContent = 'Approve';
        approveBtn.onclick = () => {
            approveTool();
            document.body.removeChild(modalOverlay);
        };

        const rejectBtn = document.createElement('button');
        rejectBtn.className = 'reject-btn';
        rejectBtn.textContent = 'Reject';
        rejectBtn.onclick = () => {
            rejectTool();
            document.body.removeChild(modalOverlay);
        };

        // Assemble the modal
        modalButtons.appendChild(approveBtn);
        modalButtons.appendChild(rejectBtn);

        modal.appendChild(modalHeader);
        modal.appendChild(modalContent);
        modal.appendChild(modalButtons);

        modalOverlay.appendChild(modal);

        // Add modal to the page
        document.body.appendChild(modalOverlay);
    }

    // Set status message
    function setStatus(message) {
        statusElement.textContent = message;
    }
});
