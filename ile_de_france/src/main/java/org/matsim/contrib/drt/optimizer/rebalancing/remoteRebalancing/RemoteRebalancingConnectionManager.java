package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingRequestBuilder;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteRebalancingConnectionManager implements IterationEndsListener {

    private final ZContext context;
    private final ZMQ.Socket socket;
    private final ObjectMapper objectMapper;
    private final RemoteRebalancingParams params;
    private final MatsimServices matsimServices;
    private final TerminationCriterion terminationCriterion;
    private final int firstIteration;
    private boolean firstConnectionMessageSent;

    public RemoteRebalancingConnectionManager(RemoteRebalancingParams params, MatsimServices matsimServices, TerminationCriterion terminationCriterion, int firstIteration) {
        this.context = new ZContext();
        this.socket = context.createSocket(SocketType.REQ);
        this.params = params;
        this.socket.connect(params.getAddress());
        this.objectMapper = new ObjectMapper();
        this.firstConnectionMessageSent = false;
        this.matsimServices = matsimServices;
        this.terminationCriterion = terminationCriterion;
        this.firstIteration = firstIteration;
    }

    public void closeConnection() {
        this.socket.close();
        this.context.close();
    }

    public void sendFirstConnection(RemoteRebalancingRequestBuilder remoteRebalancingRequestBuilder){
        if(this.firstConnectionMessageSent) {
            return;
        }
        this.firstConnectionMessageSent = true;
        try {
            String saveFile = "";
            if (this.firstIteration > 0) {
                saveFile = this.matsimServices.getControlerIO().getIterationFilename(this.firstIteration -1, "remoteRebalancerState.pkl");
                saveFile = Paths.get("").resolve(saveFile).toAbsolutePath().toString();
            }
            System.out.println("Sending Fist connection message to the rebalancing server");
            FirstConnectionMessage firstConnectionMessage = new FirstConnectionMessage(this.params.getRemoteRebalancingMethodParams().getRebalancingMethod(), this.params.getRemoteRebalancingMethodParams().getRebalancingMethodParams(), remoteRebalancingRequestBuilder.getInitializationData(), saveFile);
            String firstConnectionString = objectMapper.writeValueAsString(firstConnectionMessage);
            socket.send(firstConnectionString.getBytes(ZMQ.CHARSET), 0);
            socket.recv(0);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public RemoteRebalancingAnswer sendRebalancingRequest(RemoteRebalancingRequest request) {
        try {
            String requestString = objectMapper.writeValueAsString(request);
            socket.send(requestString.getBytes(ZMQ.CHARSET), 0);
            String reply = new String(socket.recv(0), ZMQ.CHARSET);
            return objectMapper.readValue(reply, RemoteRebalancingAnswer.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendIterationReset(String saveFilePath) {
        try {
            String message = objectMapper.writeValueAsString(new IterationReset(saveFilePath));
            socket.send(message.getBytes(ZMQ.CHARSET), 0);
            socket.recv();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void sendSaveState(String filePath) {
        try {
            String message = objectMapper.writeValueAsString(new SaveStateMessage(filePath));
            socket.send(message.getBytes(ZMQ.CHARSET), 0);
            socket.recv();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static abstract class RemoteRebalancingMessage {
        public String messageType = "abstractMessage";
    }

    public static class FirstConnectionMessage {
        public String messageType = "firstConnection";
        public String rebalancingMethod;
        public String saveFile;
        public Map<String, Object> rebalancingMethodParams;
        public Map<String, Object> initializationData;

        public FirstConnectionMessage(String rebalancingMethod, Map<String, Object> rebalancingMethodParams, Map<String, Object> initializationData, String saveFile) {
            this.rebalancingMethod = rebalancingMethod;
            this.rebalancingMethodParams = rebalancingMethodParams;
            this.initializationData = initializationData;
            this.saveFile = saveFile;
        }
    }

    public static class SaveStateMessage {
        public String messageType = "saveState";
        public String filePath;

        public SaveStateMessage(String filePath) {
            this.filePath = filePath;
        }
    }

    public static class IterationReset extends RemoteRebalancingMessage {
        public String messageType = "iterationReset";
        public final String saveFilePath;

        public IterationReset() {
            this(null);
        }

        public IterationReset(String saveFilePath) {
            this.saveFilePath = saveFilePath;
        }
    }



    public abstract static class IndividualRebalancingRequest extends HashMap<String, Object>{

        public static final String ID_ENTRY = "id";

        public IndividualRebalancingRequest(String id) {
            this.put(ID_ENTRY, id);
        }
    }

    public static class RemoteRebalancingRequest extends RemoteRebalancingMessage{
        public String messageType = "RebalancingRequest";
        public List<IndividualRebalancingRequest> vehicles = new ArrayList<>();
        public double time;

    }

    public static class RemoteRebalancingRelocation {
        public String vehicleId;
        public String linkId;
    }

    public static class RemoteRebalancingAnswer {
        public List<RemoteRebalancingRelocation> relocations = new ArrayList<>();
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        String saveFileAbsolutePath = null;
        if(!iterationEndsEvent.isLastIteration() && this.terminationCriterion.mayTerminateAfterIteration(iterationEndsEvent.getIteration()+1)) {
            String saveFileName = "remoteRebalancerState.pkl";
            Path saveFileRelativePath = Paths.get(this.matsimServices.getControlerIO().getIterationFilename(iterationEndsEvent.getIteration(), saveFileName));
            saveFileAbsolutePath = Paths.get("").resolve(saveFileRelativePath).toAbsolutePath().normalize().toString();
        }
        this.sendIterationReset(saveFileAbsolutePath);
    }
}
