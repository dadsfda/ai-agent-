package com.yupi.yuaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.yupi.yuaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private static final int MAX_TOOL_ARGUMENT_LENGTH = 1000;
    private static final String COMPACTED_TOOL_ARGUMENTS = "{\"omitted\":true}";

    private final ToolCallback[] availableTools;
    private ChatResponse toolCallChatResponse;
    private String lastAssistantResponse;
    private final ToolCallingManager toolCallingManager;
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    @Override
    public boolean think() {
        List<Message> messageList = new ArrayList<>(compactConversationHistory(getMessageList()));
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            messageList.add(new UserMessage(getNextStepPrompt()));
        }
        setMessageList(messageList);
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            String result = assistantMessage.getText();
            this.lastAssistantResponse = result;
            log.info("{} think result: {}", getName(), result);
            log.info("{} selected {} tools", getName(), toolCallList.size());
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("tool=%s, args=%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()) {
                getMessageList().add(assistantMessage);
                setState(AgentState.FINISHED);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("{} think failed", getName(), e);
            String errorMessage = "Processing error: " + e.getMessage();
            this.lastAssistantResponse = errorMessage;
            getMessageList().add(new AssistantMessage(errorMessage));
            setState(AgentState.FINISHED);
            return false;
        }
    }

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return StrUtil.blankToDefault(lastAssistantResponse, "Finished without action");
            }
            return act();
        } catch (Exception e) {
            log.error("step failed", e);
            return "Step failed: " + e.getMessage();
        }
    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "No tool call needed";
        }
        Prompt prompt = new Prompt(compactConversationHistory(getMessageList()), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        setMessageList(new ArrayList<>(compactConversationHistory(toolExecutionResult.conversationHistory())));
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "Tool " + response.name() + " result: " + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }

    List<Message> compactConversationHistory(List<Message> messageList) {
        return messageList.stream()
                .map(message -> {
                    if (!(message instanceof AssistantMessage assistantMessage) || assistantMessage.getToolCalls().isEmpty()) {
                        return message;
                    }
                    List<AssistantMessage.ToolCall> compactedToolCalls = assistantMessage.getToolCalls().stream()
                            .map(toolCall -> new AssistantMessage.ToolCall(
                                    toolCall.id(),
                                    toolCall.type(),
                                    toolCall.name(),
                                    compactToolArguments(toolCall.arguments())
                            ))
                            .toList();
                    return new AssistantMessage(
                            assistantMessage.getText(),
                            assistantMessage.getMetadata(),
                            compactedToolCalls,
                            assistantMessage.getMedia()
                    );
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String compactToolArguments(String arguments) {
        if (StrUtil.isBlank(arguments)) {
            return COMPACTED_TOOL_ARGUMENTS;
        }
        if (arguments.length() <= MAX_TOOL_ARGUMENT_LENGTH) {
            return arguments;
        }
        return COMPACTED_TOOL_ARGUMENTS;
    }
}