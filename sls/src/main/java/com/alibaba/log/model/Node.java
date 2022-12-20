package com.alibaba.log.model;

import static com.alibaba.log.model.Scene.*;

/**
 * @author qch
 * @since 2022/12/15 10:43 上午
 */
public enum Node {
    invite(inbound,true),userRTP(inbound),ivrRTP(inbound),agentWebRTC(inbound);

    private Scene scene;
    private boolean refreshPoint = false;
    Node(Scene scene) {
        this.scene = scene;
    }

    Node(Scene scene, boolean refreshPoint) {
        this.scene = scene;
        this.refreshPoint = refreshPoint;
    }

    public boolean isRefreshPoint() {
        return refreshPoint;
    }

    public Scene getScene() {
        return scene;
    }
}
