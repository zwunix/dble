/*
 * Copyright (C) 2016-2019 ActionTech.
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher.
 */

package com.actiontech.dble.backend.mysql.nio.handler.transaction.xa;

import com.actiontech.dble.backend.BackendConnection;
import com.actiontech.dble.route.RouteResultsetNode;
import com.actiontech.dble.server.NonBlockingSession;

import java.util.List;

public class XAAutoRollbackNodesHandler extends XARollbackNodesHandler {
    private RouteResultsetNode[] nodes;
    private List<BackendConnection> errConnection;

    public XAAutoRollbackNodesHandler(NonBlockingSession session, byte[] packet, RouteResultsetNode[] nodes, List<BackendConnection> errConnection) {
        super(session);
        this.sendData = packet;
        this.nodes = nodes;
        this.errConnection = errConnection;
    }

    @Override
    public void rollback() {
        if (errConnection != null && nodes.length == errConnection.size()) {
            for (BackendConnection conn : errConnection) {
                conn.close("rollback all connection error");
            }
            errConnection.clear();
            session.getSource().write(sendData);
            return;
        }
        if (errConnection != null && errConnection.size() > 0) {
            for (RouteResultsetNode node : nodes) {
                final BackendConnection conn = session.getTarget(node);
                if (errConnection.contains(conn)) {
                    session.getTargetMap().remove(node);
                    conn.close("errorConnection of rollback");
                }
            }
            errConnection.clear();
        }
        super.rollback();
    }

    @Override
    protected void setResponseTime(boolean isSuccess) {
        session.setResponseTime(isSuccess);
    }
}
