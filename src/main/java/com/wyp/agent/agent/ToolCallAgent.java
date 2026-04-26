package com.wyp.agent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.wyp.agent.agent.model.AgentState;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private static final int MAX_TOOL_ARGUMENT_LENGTH = 1000;
    private static final int MAX_TOOL_RESPONSE_LENGTH = 4000;
    private static final String COMPACTED_TOOL_ARGUMENTS = "{\"omitted\":true}";
    private static final String COMPACTED_TOOL_RESPONSE = "{\"omitted\":true}";
    private static final String TERMINATE_TOOL_NAME = "doTerminate";
    private static final String TERMINATE_PLACEHOLDER = "\u4efb\u52a1\u7ed3\u675f";
    private static final String TOOL_PREFIX = "\u8c03\u7528\u5de5\u5177: ";
    private static final String NO_TOOL = "\u65e0";
    private static final String PDF_READY_PREFIX = "PDF_READY:";

    private final ToolCallback[] availableTools;
    private final List<String> postStepOutputs = new ArrayList<>();
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
            this.lastAssistantResponse = assistantMessage.getText();
            log.info("{} think result: {}", getName(), lastAssistantResponse);
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
        List<ToolResponseMessage.ToolResponse> toolResponses = toolResponseMessage.getResponses();
        collectPostStepOutputs(toolResponses);
        boolean terminateToolCalled = toolResponses.stream()
                .anyMatch(response -> TERMINATE_TOOL_NAME.equals(response.name()));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
            String finalResponse = extractFinalResponse(toolResponses, lastAssistantResponse);
            if (StrUtil.isNotBlank(finalResponse)) {
                postStepOutputs.add(finalResponse);
            }
        }
        AssistantMessage assistantMessage = toolCallChatResponse.getResult().getOutput();
        String summary = summarizeToolStep(assistantMessage.getToolCalls(), toolResponses);
        log.info(summary);
        return summary;
    }

    String summarizeToolStep(List<AssistantMessage.ToolCall> toolCalls,
                             List<ToolResponseMessage.ToolResponse> toolResponses) {
        return TOOL_PREFIX + summarizeToolNames(toolCalls, toolResponses);
    }

    String extractFinalResponse(List<ToolResponseMessage.ToolResponse> toolResponses, String assistantResponse) {
        String toolResponse = "";
        if (toolResponses != null) {
            toolResponse = toolResponses.stream()
                    .filter(response -> TERMINATE_TOOL_NAME.equals(response.name()))
                    .map(ToolResponseMessage.ToolResponse::responseData)
                    .map(this::normalizeToolResponse)
                    .filter(StrUtil::isNotBlank)
                    .filter(responseData -> !TERMINATE_PLACEHOLDER.equals(responseData))
                    .findFirst()
                    .orElse("");
        }
        if (StrUtil.isNotBlank(toolResponse)) {
            return toolResponse;
        }
        return StrUtil.blankToDefault(StrUtil.trim(assistantResponse), "");
    }

    void collectPostStepOutputs(List<ToolResponseMessage.ToolResponse> toolResponses) {
        if (toolResponses == null) {
            return;
        }
        toolResponses.stream()
                .map(ToolResponseMessage.ToolResponse::responseData)
                .map(this::normalizeToolResponse)
                .filter(responseData -> responseData.startsWith(PDF_READY_PREFIX))
                .forEach(postStepOutputs::add);
    }

    List<Message> compactConversationHistory(List<Message> messageList) {
        return messageList.stream()
                .map(message -> {
                    if (message instanceof AssistantMessage assistantMessage && !assistantMessage.getToolCalls().isEmpty()) {
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
                    }
                    if (message instanceof ToolResponseMessage toolResponseMessage) {
                        List<ToolResponseMessage.ToolResponse> compactedResponses = toolResponseMessage.getResponses().stream()
                                .map(toolResponse -> new ToolResponseMessage.ToolResponse(
                                        toolResponse.id(),
                                        toolResponse.name(),
                                        compactToolResponseData(toolResponse.responseData())
                                ))
                                .toList();
                        return new ToolResponseMessage(compactedResponses, toolResponseMessage.getMetadata());
                    }
                    return message;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    protected List<String> consumePostStepOutputs() {
        List<String> outputs = new ArrayList<>(postStepOutputs);
        postStepOutputs.clear();
        return outputs;
    }

    private String summarizeToolNames(List<AssistantMessage.ToolCall> toolCalls,
                                      List<ToolResponseMessage.ToolResponse> toolResponses) {
        Set<String> toolNames = new LinkedHashSet<>();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            toolCalls.stream()
                    .map(AssistantMessage.ToolCall::name)
                    .filter(StrUtil::isNotBlank)
                    .forEach(toolNames::add);
        } else if (toolResponses != null) {
            toolResponses.stream()
                    .map(ToolResponseMessage.ToolResponse::name)
                    .filter(StrUtil::isNotBlank)
                    .forEach(toolNames::add);
        }
        if (toolNames.isEmpty()) {
            return NO_TOOL;
        }
        return String.join("、", toolNames);
    }

    private String normalizeToolResponse(String responseData) {
        if (StrUtil.isBlank(responseData)) {
            return "";
        }
        String normalized = StrUtil.trim(responseData);
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return StrUtil.unWrap(normalized, '"');
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

    private String compactToolResponseData(String responseData) {
        if (StrUtil.isBlank(responseData)) {
            return COMPACTED_TOOL_RESPONSE;
        }
        if (responseData.length() <= MAX_TOOL_RESPONSE_LENGTH) {
            return responseData;
        }
        return COMPACTED_TOOL_RESPONSE;
    }
}
