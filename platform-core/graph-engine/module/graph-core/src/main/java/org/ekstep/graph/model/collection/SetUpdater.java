package org.ekstep.graph.model.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.ekstep.common.dto.Request;
import org.ekstep.common.dto.Response;
import org.ekstep.graph.common.mgr.BaseGraphManager;
import org.ekstep.graph.dac.enums.GraphDACParams;
import org.ekstep.graph.dac.model.Node;
import org.ekstep.graph.dac.model.Relation;
import org.ekstep.graph.model.AbstractDomainObject;
import org.ekstep.graph.model.node.MetadataNode;
import org.ekstep.graph.model.node.ValueNode;
import org.ekstep.graph.model.relation.HasValueRelation;
import org.ekstep.graph.model.relation.UsedBySetRelation;

import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;

public class SetUpdater extends AbstractDomainObject {

    public SetUpdater(BaseGraphManager manager, String graphId) {
        super(manager, graphId);
    }

    public Future<List<String>> getSets(final Request req, String objectType, Map<String, Object> oldMetadata,
            Map<String, Object> newMetadata) {
        final ExecutionContext ec = manager.getContext().dispatcher();
        if (null == oldMetadata)
            oldMetadata = new HashMap<String, Object>();
        if (null == newMetadata)
            newMetadata = new HashMap<String, Object>();
        List<Future<List<String>>> list = new ArrayList<Future<List<String>>>();
        for (Entry<String, Object> entry : newMetadata.entrySet()) {
            Future<List<String>> setIdsFuture = getSets(req, objectType, entry.getKey(), oldMetadata.get(entry.getKey()), entry.getValue());
            list.add(setIdsFuture);
        }
        for (Entry<String, Object> entry : oldMetadata.entrySet()) {
            if (null == newMetadata.get(entry.getKey())) {
                Future<List<String>> setIdsFuture = getSets(req, objectType, entry.getKey(), entry.getValue(),
                        newMetadata.get(entry.getKey()));
                list.add(setIdsFuture);
            }
        }
        Future<Iterable<List<String>>> composite = Futures.sequence(list, ec);
        Future<List<String>> future = composite.map(new Mapper<Iterable<List<String>>, List<String>>() {
            @Override
            public List<String> apply(Iterable<List<String>> parameter) {
                List<String> setIds = new ArrayList<String>();
                if (null != parameter) {
                    for (List<String> list : parameter) {
                        if (null != list && !list.isEmpty())
                            setIds.addAll(list);
                    }
                }
                return setIds;
            }
        }, ec);
        return future;
    }

    public Future<List<String>> getSets(final Request req, String objectType, String property, final Object oldValue, final Object newValue) {
        final Promise<List<String>> promise = Futures.promise();
        final Future<List<String>> future = promise.future();
        MetadataNode mNode = new MetadataNode(getManager(), getGraphId(), objectType, property);
        
        Node node = getNodeObject(req, mNode.getNodeId());
        final List<String> setIds = new ArrayList<String>();
        if (null != node) {
            List<String> valueNodeIds = new ArrayList<String>();
            List<Relation> outRels = node.getOutRelations();
            if (null != outRels && outRels.size() > 0) {
                for (Relation rel : outRels) {
                    if (StringUtils.equals(UsedBySetRelation.RELATION_NAME, rel.getRelationType())) {
                        setIds.add(rel.getEndNodeId());
                    } else if (StringUtils.equals(HasValueRelation.RELATION_NAME, rel.getRelationType())) {
                        if (null != rel.getEndNodeMetadata()) {
                            Object val = rel.getEndNodeMetadata().get(ValueNode.VALUE_NODE_VALUE_KEY);
                            if (null != val) {
                                if (val == oldValue || val == newValue) {
                                    valueNodeIds.add(rel.getEndNodeId());
                                }
                            }
                        }
                    }
                }
            }
            if (!valueNodeIds.isEmpty()) {
                List<Node> vNodes = new ArrayList<Node>();
                for (String vNodeId : valueNodeIds) {
                    Node vNodeFuture = getNodeObject(req, vNodeId);
                    vNodes.add(vNodeFuture);
                }

				if (null != vNodes) {
					for (Node vNode : vNodes) {
						List<Relation> outRels1 = vNode.getOutRelations();
						if (null != outRels1 && outRels1.size() > 0) {
							for (Relation rel : outRels1) {
								if (StringUtils.equals(UsedBySetRelation.RELATION_NAME, rel.getRelationType())) {
									setIds.add(rel.getEndNodeId());
                                }
                            }
                        }
                    }
				}
				promise.success(setIds);
            } else {
                promise.success(setIds);
            }
        } else {
            promise.success(setIds);
        }
        return future;
    }

	private Node getNodeObject(Request req, String nodeId) {
        Request request = new Request(req);
        request.put(GraphDACParams.node_id.name(), nodeId);

		Response res = searchMgr.getNodeByUniqueId(request);

		if (!manager.checkError(res)) {
			Node node = (Node) res.get(GraphDACParams.node.name());
			return node;
		}
		return null;

    }

}