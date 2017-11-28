/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkWW.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package masterThesis.drt.eventsrouting;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.*;

/**
 * Transit router network with travel, transfer, and waiting links
 * 
 * @author sergioo
 */

public final class TransitRouterNetworkWW implements Network {

	private final static Logger log = Logger.getLogger(TransitRouterNetworkWW.class);
	
	private final Map<Id<Link>, TransitRouterNetworkLink> links = new LinkedHashMap<Id<Link>, TransitRouterNetworkLink>();
	private final Map<Id<Node>, TransitRouterNetworkNode> nodes = new LinkedHashMap<Id<Node>, TransitRouterNetworkNode>();
	protected QuadTree<TransitRouterNetworkNode> qtNodes = null;

	private long nextNodeId = 0;
	protected long nextLinkId = 0;

	public static final class TransitRouterNetworkNode implements Node {

		public final TransitStopFacility stop;
		final Id<Node> id;
		final Map<Id<Link>, TransitRouterNetworkLink> ingoingLinks = new LinkedHashMap<Id<Link>, TransitRouterNetworkLink>();
		final Map<Id<Link>, TransitRouterNetworkLink> outgoingLinks = new LinkedHashMap<Id<Link>, TransitRouterNetworkLink>();

		public TransitRouterNetworkNode(final Id<Node> id, final TransitStopFacility stop) {
			this.id = id;
			this.stop = stop;
		}

		@Override
		public Map<Id<Link>, ? extends Link> getInLinks() {
			return this.ingoingLinks;
		}

		@Override
		public Map<Id<Link>, ? extends Link> getOutLinks() {
			return this.outgoingLinks;
		}

		@Override
		public boolean addInLink(final Link link) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addOutLink(final Link link) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Coord getCoord() {
			return this.stop.getCoord();
		}

		@Override
		public Id<Node> getId() {
			return this.id;
		}

		public TransitStopFacility getStop() {
			return stop;
		}

		@Override
		public Link removeInLink(Id<Link> linkId) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Link removeOutLink(Id<Link> outLinkId) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public void setCoord(Coord coord) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Attributes getAttributes() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Looks to me like an implementation of the Link interface, with get(Transit)Route and get(Transit)Line on top.
	 * To recall: TransitLine is something like M44.  But it can have more than one route, e.g. going north, going south,
	 * long route, short route. That is, presumably we have one such TransitRouterNetworkLink per TransitRoute. kai/manuel, feb'12
	 */
	public static final class TransitRouterNetworkLink implements Link {

		final TransitRouterNetworkNode fromNode;
		final TransitRouterNetworkNode toNode;
		final Id<Link> id;
		private double length;

		public TransitRouterNetworkLink(final Id<Link> id, final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode) {
			this.id = id;
			this.fromNode = fromNode;
			this.toNode = toNode;
			this.length = CoordUtils.calcEuclideanDistance(this.toNode.stop.getCoord(), this.fromNode.stop.getCoord());
		}

		@Override
		public TransitRouterNetworkNode getFromNode() {
			return this.fromNode;
		}

		@Override
		public TransitRouterNetworkNode getToNode() {
			return this.toNode;
		}

		@Override
		public double getCapacity() {
			return getCapacity(Time.UNDEFINED_TIME);
		}

		@Override
		public double getCapacity(final double time) {
			return 9999;
		}

		@Override
		public double getFreespeed() {
			return getFreespeed(Time.UNDEFINED_TIME);
		}

		@Override
		public double getFreespeed(final double time) {
			return 10;
		}

		@Override
		public Id<Link> getId() {
			return this.id;
		}

		@Override
		public double getNumberOfLanes() {
			return getNumberOfLanes(Time.UNDEFINED_TIME);
		}

		@Override
		public double getNumberOfLanes(final double time) {
			return 1;
		}

		@Override
		public double getLength() {
			return this.length;
		}

		@Override
		public void setCapacity(final double capacity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setFreespeed(final double freespeed) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setFromNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setNumberOfLanes(final double lanes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setLength(final double length) {
			this.length = length;
		}

		@Override
		public boolean setToNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Coord getCoord() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> getAllowedModes() {
			return null;
		}

		@Override
		public void setAllowedModes(final Set<String> modes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getFlowCapacityPerSec() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public double getFlowCapacityPerSec(double time) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Attributes getAttributes() {
			throw new UnsupportedOperationException();
		}
	}
	public TransitRouterNetworkNode createNode(final TransitStopFacility stop, boolean wait) {
		Id<Node> id = null;
		id = Id.createNodeId(stop.getId().toString()+(wait?"_W":""));
		final TransitRouterNetworkNode node = new TransitRouterNetworkNode(id, stop);
		if(this.nodes.get(node.getId())!=null)
			throw new RuntimeException();
		this.nodes.put(node.getId(), node);
		return node;
	}

	public TransitRouterNetworkLink createLink(final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode) {
		final TransitRouterNetworkLink link = new TransitRouterNetworkLink(Id.createLinkId(this.nextLinkId++), fromNode, toNode);
		this.links.put(link.getId(), link);
		fromNode.outgoingLinks.put(link.getId(), link);
		toNode.ingoingLinks.put(link.getId(), link);
		return link;
	}
	@Override
	public Map<Id<Node>, TransitRouterNetworkNode> getNodes() {
		return this.nodes;
	}
	@Override
	public Map<Id<Link>, TransitRouterNetworkLink> getLinks() {
		return this.links;
	}
	public void finishInit() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (TransitRouterNetworkNode node : getNodes().values()) {
			Coord c = node.stop.getCoord();
			if (c.getX() < minX)
				minX = c.getX();
			if (c.getY() < minY)
				minY = c.getY();
			if (c.getX() > maxX)
				maxX = c.getX();
			if (c.getY() > maxY)
				maxY = c.getY();
		}
		QuadTree<TransitRouterNetworkNode> quadTree = new QuadTree<TransitRouterNetworkNode>(minX, minY, maxX, maxY);
		for (TransitRouterNetworkNode node : getNodes().values()) {
			Coord c = node.stop.getCoord();
			quadTree.put(c.getX(), c.getY(), node);
		}
		this.qtNodes = quadTree;
	}
	public static TransitRouterNetworkWW createFromStops(final TransitSchedule schedule) {
		log.info("start creating transit network");
		final TransitRouterNetworkWW transitNetwork = new TransitRouterNetworkWW();
		final Counter linkCounter = new Counter(" link #");
		final Counter nodeCounter = new Counter(" node #");
		int numTravelLinks = 0, numWaitingLinks = 0;
		Map<Id<TransitStopFacility>, TransitRouterNetworkNode> stops = new HashMap<Id<TransitStopFacility>, TransitRouterNetworkNode>();
		TransitRouterNetworkNode nodeA, nodeB;
		log.info("add nodes and waiting links");
		// build stop nodes
		for (TransitStopFacility stop:schedule.getFacilities().values()) {
			nodeB = transitNetwork.createNode(stop, true);
			nodeCounter.incCounter();
			stops.put(Id.create(stop.getId().toString()+"_W",TransitStopFacility.class), nodeB);
		}
		transitNetwork.finishInit();
		for (TransitStopFacility stop:schedule.getFacilities().values()) {
			nodeA = transitNetwork.createNode(stop, false);
			nodeCounter.incCounter();
			stops.put(stop.getId(), nodeA);
			nodeB = stops.get(Id.create(stop.getId().toString()+"_W",TransitStopFacility.class));
			transitNetwork.createLink(nodeB, nodeA);
			linkCounter.incCounter();
			numWaitingLinks++;
		}
		// build travel links
		log.info("add travel links");
		for(TransitStopFacility stopA:schedule.getFacilities().values())
			for(TransitStopFacility stopB:schedule.getFacilities().values())
				if(stopA!=stopB) {
					nodeA = stops.get(stopA.getId());
					nodeB = stops.get(Id.create(stopB.getId().toString()+"_W",TransitStopFacility.class));
					transitNetwork.createLink(nodeA, nodeB);
					linkCounter.incCounter();
					numWaitingLinks++;
				}
		log.info("transit router network statistics:");
		log.info(" # nodes: " + transitNetwork.getNodes().size());
		log.info(" # links total:     " + transitNetwork.getLinks().size());
		log.info(" # travel links:  " + numTravelLinks);
		log.info(" # waiting links:  " + numWaitingLinks);
		return transitNetwork;
	}
	public Collection<TransitRouterNetworkNode> getNearestNodes(final Coord coord, final double distance) {
		return this.qtNodes.getDisk(coord.getX(), coord.getY(), distance);
	}

	public TransitRouterNetworkNode getNearestNode(final Coord coord) {
		return this.qtNodes.getClosest(coord.getX(), coord.getY());
	}

	@Override
	public double getCapacityPeriod() {
		return 3600.0;
	}

	@Override
	public NetworkFactory getFactory() {
		return null;
	}

	@Override
	public double getEffectiveLaneWidth() {
		return 3;
	}

	@Override
	public void addNode(Node nn) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addLink(Link ll) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Link removeLink(Id<Link> linkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node removeNode(Id<Node> nodeId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCapacityPeriod(double capPeriod) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setEffectiveCellSize(double effectiveCellSize) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setEffectiveLaneWidth(double effectiveLaneWidth) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public double getEffectiveCellSize() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}
}
