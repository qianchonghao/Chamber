package com.alibaba.log;

import com.google.common.collect.Lists;

import java.util.List;



import static com.alibaba.log.Node.*;

/**
 * @author qch
 * @since 2022/12/15 10:34 上午
 */
public enum Scene {
    inbound(Lists.newArrayList(invite,userRTP,ivrRTP,agentWebRTC));

    private List<Node> nodes;

    Scene(List<Node> nodes) {
        this.nodes = nodes;
    }

    public List<Node> getNodes() {
        return nodes;
    }
}
