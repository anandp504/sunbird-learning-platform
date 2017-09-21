package com.ilimi.graph.common.mgr;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

import com.ilimi.common.Platform;
import com.ilimi.common.logger.LoggerEnum;
import com.ilimi.common.logger.PlatformLogger;

public class Configuration {

    public static long TIMEOUT = 30000;
    public static List<String> graphIds = new ArrayList<String>();
    
    static {
    	try{
            int timeout = Platform.config.getInt("akka.request_timeout");
            if (timeout > 0) {
                TIMEOUT = timeout * 1000;
            }
            String ids = Platform.config.getString("graph.ids");
            if (StringUtils.isNotBlank(ids)) {
            	String[] array = ids.split(",");
            	if (null != array && array.length > 0) {
            		for (String id : array) {
            			if (StringUtils.isNotBlank(id))
            				graphIds.add(id);
            		}
            	}
            }
        } catch (Exception e) {
        	PlatformLogger.log("Error! While Loading Graph Properties.", e.getMessage(),LoggerEnum.ERROR.name());
        }
    }
    
    public static void registerNewGraph(String graphId){
    	graphIds.add(graphId);
    }
}
