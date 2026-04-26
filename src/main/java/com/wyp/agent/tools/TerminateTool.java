package com.wyp.agent.tools;

import cn.hutool.core.util.StrUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class TerminateTool {

    @Tool(description = "Terminate the interaction after all tasks are done. Always provide the final user-facing response in `finalResponse`.")
    public String doTerminate(@ToolParam(description = "Final response that should be shown to the user") String finalResponse) {
        return StrUtil.blankToDefault(finalResponse, "任务结束");
    }
}