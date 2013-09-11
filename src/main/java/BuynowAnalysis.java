import com.jcraft.jsch.JSchException;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuynowAnalysis {


    private PrintWriter bw;
    private OutputStreamWriter osw;
    private FileOutputStream fos;
    private BufferedReader br = null;
    private InputStreamReader isr = null;
    private FileInputStream fis = null;
    List errorInfos = new ArrayList<ErrorInfo>();
    int totalLines = 0;
    Map analysis = new HashMap<String, Integer>();
    Set<String> errorTypes = new HashSet<String>();
    Map<String, Integer> temp = new LinkedHashMap<String, Integer>();

    //configuration
    private String errorTag = null;
    private String startOfBlockTag = null;

    private String appname;
    private String ip;
    private int port;
    private String password;
    private String remoteFile;
    private String localFile;
    private String outFileName;
    private String specifiedErrorTypes;
    private String requireRemoteGet;

    public void process() throws UnsupportedEncodingException {

        prepareProperties();

        try {
            if (Boolean.parseBoolean(requireRemoteGet)) {
                JSchUtil jSchUtil = new JSchUtil(appname, ip, port, password);
                jSchUtil.get(remoteFile, localFile);
            }
            fis = new FileInputStream(localFile);
            isr = new InputStreamReader(fis, "utf-8");
            br = new BufferedReader(isr);
            fos = new FileOutputStream(outFileName);
            osw = new OutputStreamWriter(fos, "utf-8");
            bw = new PrintWriter(osw);


            loadErrorInfoIntoMemLikeErrorInfoClass();
            Collections.sort(errorInfos, new ErrorTypeComparator());
            analysisAppearNum();
            sort();
            writeAnalysis();
            writeFirstLine();
            writeBlock();

            br.close();
            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSchException e) {
            e.printStackTrace();
        }


    }

    private void prepareProperties() throws UnsupportedEncodingException {
        Configuration rc = new Configuration("./buynowAnalysis/conf/analysis.properties");

        startOfBlockTag = rc.getValue("startOfBlockTag").trim();
        errorTag = rc.getValue("errorTag").trim();
        appname = rc.getValue("appname").trim();
        ip = rc.getValue("ip").trim();
        port = Integer.parseInt(rc.getValue("port").trim());
        password = rc.getValue("password").trim();
        remoteFile = rc.getValue("remoteFile").trim();
        localFile = rc.getValue("localFile").trim();
        outFileName = rc.getValue("outFileName");
        specifiedErrorTypes = rc.getValue("specifiedErrorTypes");
        requireRemoteGet = rc.getValue("requireRemoteGet").trim();

    }

    private void writeBlock() {
        bw.println("                ***** This section is the Block section *****");
        for (int i = 0; i < 5; i++) {
            bw.println();
        }
        Iterator<ErrorInfo> infoIterator = errorInfos.iterator();
        while (infoIterator.hasNext()) {
            ErrorInfo errorInfo = infoIterator.next();
            bw.print(errorInfo.getBlock());
        }
        for (int i = 0; i < 5; i++) {
            bw.println();
        }
        bw.flush();

    }

    private void writeFirstLine() {
        bw.println("                         ***** This section is the FirstLine section *****");
        for (int i = 0; i < 5; i++) {
            bw.println();
        }
        Iterator<ErrorInfo> infoIterator = errorInfos.iterator();
        while (infoIterator.hasNext()) {
            ErrorInfo errorInfo = infoIterator.next();
            bw.println(errorInfo.getFirstLine());
        }
        for (int i = 0; i < 5; i++) {
            bw.println();
        }
        bw.flush();

    }

    private void writeAnalysis() {
        Set<String> set = analysis.keySet();
        Iterator<String> iterator = set.iterator();
        bw.println("                        ***** This section is the number analysis section *****");
        for (int i = 0; i < 5; i++) {
            bw.println();
        }
        while (iterator.hasNext()) {
            String key = iterator.next();
            if ((Integer) analysis.get(key) > 0) {
                bw.println(key);
                bw.println(analysis.get(key));
            }
        }
        for (int i = 0; i < 5; i++) {
            bw.println();
        }
        bw.flush();
    }

    private void sort() {
        for (int i = 0; i < analysis.size(); i++) {
            Set<String> set1 = analysis.keySet();
            Iterator<String> iterator1 = set1.iterator();
            String key = iterator1.next();
            Integer integer = (Integer) analysis.get(key);

            Set<String> set = analysis.keySet();
            Iterator<String> iterator = set.iterator();
            while (iterator.hasNext()) {
                String keyTemp = iterator.next();
                Integer integerTemp = (Integer) analysis.get(keyTemp);
                if (integer < integerTemp) {
                    key = keyTemp;
                    integer = integerTemp;
                }
            }
            temp.put(key, integer);
            analysis.remove(key);
        }

        analysis = temp;

    }


    private void analysisAppearNum() {
        Iterator<ErrorInfo> infoIterator = errorInfos.iterator();
        while (infoIterator.hasNext()) {
            ErrorInfo errorInfo = infoIterator.next();
            if (!analysis.keySet().contains(errorInfo.getErrorType())) {
                analysis.put(errorInfo.getErrorType(), new Integer(1));
            } else {
                int j = (Integer) analysis.get(errorInfo.getErrorType());
                analysis.put(errorInfo.getErrorType(), new Integer(++j));
            }
        }
    }


    private void loadErrorInfoIntoMemLikeErrorInfoClass() {
        String line;
        try {
            boolean firstBlock = true;
            String block = null;
            String firstLine = null;
            String errorType = null;

            while ((line = br.readLine()) != null) {
                totalLines++;
                line = line.trim();
                if (firstBlock) {
                    if (isStartOfBlock(line)) {
                        firstLine = line;
                        errorType = getErrorType(firstLine);
                        block = line + "\r\n";
                    } else {
                        block = block + line + "\r\n";
                    }
                    firstBlock = false;
                } else {
                    if (isStartOfBlock(line)) {
                        errorInfos.add(new ErrorInfo(errorType, firstLine, block));
                        firstLine = line;
                        errorType = getErrorType(firstLine);
                        block = line + "\r\n";
                    } else {
                        block = block + line + "\r\n";
                    }
                }

            }
            errorInfos.add(new ErrorInfo(errorType, firstLine, block));
            removeInvalidErrorInfo();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeInvalidErrorInfo() {
        List<ErrorInfo> list = new ArrayList<ErrorInfo>();
        Iterator<ErrorInfo> infoIterator = errorInfos.iterator();
        while (infoIterator.hasNext()) {
            ErrorInfo errorInfo = infoIterator.next();
            if (!containERROR(errorInfo.getFirstLine())) {
                list.add(errorInfo);
            }
        }
        Iterator<ErrorInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            ErrorInfo errorInfo = iterator.next();
            errorInfos.remove(errorInfo);
        }


    }

    private String getErrorType(String firstLine) {
        String[] temp = StringUtils.split(firstLine, "-");
        String[] specifiedErrorTypeses = StringUtils.split(specifiedErrorTypes, " ");
        if (temp.length == 3) {
            for (int i = 0; i < specifiedErrorTypeses.length; i++) {
                if (isThisPattern(specifiedErrorTypeses[i], temp[2].trim())) {
                    errorTypes.add(specifiedErrorTypeses[i]);
                    return specifiedErrorTypeses[i];
                }
            }
            errorTypes.add(temp[2].trim());
            return temp[2].trim();

        } else {
            return "no clear error type";
        }

    }

    private boolean containERROR(String line) {
        return isThisPattern(errorTag, line);
    }


    private void show() {
       /* Iterator<String> iterator = errorTypes.iterator();
        while(iterator.hasNext()){
            String type = iterator.next();
            System.out.println(type);
        }*/
        Iterator<ErrorInfo> infoIterator = errorInfos.iterator();
        while (infoIterator.hasNext()) {
            ErrorInfo errorInfo = infoIterator.next();
            System.out.println(errorInfo.getFirstLine());

        }
    }


    public boolean isStartOfBlock(String line) {
        return isThisPattern(startOfBlockTag, line);
    }

    public boolean isThisPattern(String pattern, String line) {
        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher = pattern1.matcher(line);
        return matcher.find();
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
       // URL path = BuynowAnalysis.class.getClassLoader().getResource("立即购买.log");
       // System.out.println(path);

        //System.out.println(System.getProperty("user.dir"));
        //URL path = BuynowAnalysis.class.getResource("");
        //System.out.println(path);
       BuynowAnalysis buynowAnalysis = new BuynowAnalysis();
       buynowAnalysis.process();
    }

}

