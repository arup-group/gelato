package com.arup.cml.abm.kpi.builders;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class NetworkBuilder {
    Network network = NetworkUtils.createNetwork();
    double defaultLinkLength = 10;
    double defaultLinkSpeed = 10;
    double defaultLinkCapacity = 300;
    int defaultLinkLanes = 1;

    public NetworkBuilder withNetworkLink(String id, String fromNode, String toNode, double length, double speed, double capacity, int lanes) {
        Link link = network.getFactory().createLink(
                Id.create(id, Link.class),
                network.getNodes().get(Id.create(fromNode, Node.class)),
                network.getNodes().get(Id.create(toNode, Node.class))
        );
        link.setLength(length);
        link.setFreespeed(speed);
        link.setCapacity(capacity);
        link.setNumberOfLanes(lanes);
        network.addLink(link);
        return this;
    }

    public NetworkBuilder withNetworkLink(String id, String fromNode, String toNode) {
        return withNetworkLink(id, fromNode, toNode, defaultLinkLength, defaultLinkSpeed, defaultLinkCapacity, defaultLinkLanes);
    }

    public NetworkBuilder withNetworkLinkWithLength(String id, String fromNode, String toNode, double length) {
        return withNetworkLink(id, fromNode, toNode, length, defaultLinkSpeed, defaultLinkCapacity, defaultLinkLanes);
    }

    public NetworkBuilder withNetworkLinkWithSpeed(String id, String fromNode, String toNode, double speed) {
        return withNetworkLink(id, fromNode, toNode, defaultLinkLength, speed, defaultLinkCapacity, defaultLinkLanes);
    }

    public NetworkBuilder withNetworkNode(String id, double x, double y) {
        Node node = network.getFactory().createNode(Id.create(id, Node.class), new Coord(x, y));
        network.addNode(node);
        return this;
    }

    public Network build() {
        return this.network;
    }
}
