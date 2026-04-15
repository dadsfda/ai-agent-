package com.yupi.yuaiagent.agent;

import com.yupi.yuaiagent.tools.FileOperationTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;

class ToolCallAgentTest {

    @Test
    void thinkShouldSupportToolCallbacksWithoutTreatingThemAsAnnotatedTools() {
        ToolCallback[] toolCallbacks = ToolCallbacks.from(new FileOperationTool());
        ToolCallAgent agent = new ToolCallAgent(toolCallbacks);
        agent.setName("test-agent");
        agent.setSystemPrompt("system");
        agent.setNextStepPrompt("next");
        agent.setChatClient(ChatClient.builder(new FakeChatModel("done")).build());

        boolean shouldAct = agent.think();

        Assertions.assertFalse(shouldAct);
        AssistantMessage assistantMessage = (AssistantMessage) agent.getMessageList().get(agent.getMessageList().size() - 1);
        Assertions.assertFalse(assistantMessage.getText().contains("No @Tool annotated methods found"));
        Assertions.assertEquals("done", assistantMessage.getText());
    }

    @Test
    void runShouldStopImmediatelyWhenModelReturnsDirectAnswerWithoutTools() {
        ToolCallback[] toolCallbacks = ToolCallbacks.from(new FileOperationTool());
        ToolCallAgent agent = new ToolCallAgent(toolCallbacks);
        agent.setName("test-agent");
        agent.setSystemPrompt("system");
        agent.setNextStepPrompt("next");
        agent.setMaxSteps(5);
        agent.setChatClient(ChatClient.builder(new FakeChatModel("hello")).build());

        String result = agent.run("hello");

        Assertions.assertEquals("Step 1: hello", result);
        long nextStepPromptCount = agent.getMessageList().stream()
                .filter(message -> message instanceof UserMessage)
                .map(message -> ((UserMessage) message).getText())
                .filter("next"::equals)
                .count();
        Assertions.assertEquals(1, nextStepPromptCount);
    }

    @Test
    void thinkShouldWorkWhenHistoryWasCompactedToImmutableList() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[0]);
        agent.setName("test-agent");
        agent.setSystemPrompt("system");
        agent.setNextStepPrompt("next");
        agent.setMessageList(List.of(new UserMessage("previous")));
        agent.setChatClient(ChatClient.builder(new FakeChatModel("continue")).build());

        Assertions.assertDoesNotThrow(agent::think);
        Assertions.assertInstanceOf(ArrayList.class, agent.getMessageList());
    }

    @Test
    void shouldCompactMediumSizedToolArgumentsBeforeNextRound() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[0]);
        String largeArguments = "{\"content\":\"" + "A".repeat(1500) + "\"}";
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-2", "function", "writeFile", largeArguments);
        AssistantMessage assistantMessage = new AssistantMessage("prepare", java.util.Map.of(), List.of(toolCall));

        List<Message> compacted = agent.compactConversationHistory(List.of(assistantMessage));
        AssistantMessage compactedMessage = (AssistantMessage) compacted.get(0);
        AssistantMessage.ToolCall compactedToolCall = compactedMessage.getToolCalls().get(0);

        Assertions.assertEquals("{\"omitted\":true}", compactedToolCall.arguments());
    }

    @Test
    void shouldCompactOversizedToolArgumentsBeforeNextRound() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[0]);
        String oversizedArguments = "{\"content\":\"" + "B".repeat(5000) + "\"}";
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "function", "writeFile", oversizedArguments);
        AssistantMessage assistantMessage = new AssistantMessage("written", java.util.Map.of(), List.of(toolCall));

        List<Message> compacted = agent.compactConversationHistory(List.of(assistantMessage));
        AssistantMessage compactedMessage = (AssistantMessage) compacted.get(0);
        AssistantMessage.ToolCall compactedToolCall = compactedMessage.getToolCalls().get(0);

        Assertions.assertEquals("{\"omitted\":true}", compactedToolCall.arguments());
        Assertions.assertEquals("writeFile", compactedToolCall.name());
    }

    static class FakeChatModel implements ChatModel {

        private final String responseText;

        FakeChatModel(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responseText))));
        }
    }
}