package com.shikong.controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shikong.model.Message;
import com.shikong.utils.HttpUtils;
import com.shikong.utils.MessageUtil;
import com.shikong.utils.TokenUtils;
import org.apache.log4j.Logger;


public class Inter extends HttpServlet {

    private String serverUrl;

    private String appID;

    private String token;

    //日志文件
    private static Logger logger = Logger.getLogger(Inter.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String method = (String) req.getAttribute("method");
        String serverName = (String) req.getAttribute("serverName");
        String data = (String) req.getAttribute("data");

        HttpUtils httpUtils = new HttpUtils();

        String result = "";
        try {
            HttpURLConnection con = null;

//            String url = serverUrl+"snt/"+serverName + MessageUtil.agPath(appID, token);
            String url = serverUrl+serverName;
            logger.trace("the url : "+url);
            if(HttpUtils.METHOD_GET.equals(method)){

                String dataParams = "";

                Set<String> dataSet = new HashSet<String>();

                if(!"".equals(data) && data!= null){
                    JSONObject paramObject = JSON.parseObject(data);

                    dataSet = paramObject.keySet();

                    if(dataSet.size() > 0 ){
                        for (String key : dataSet) {
                            dataParams +=  key + "=" + URLEncoder.encode(paramObject.getString(key),"utf-8")+"&";
                        }
                    }
                }

                con = httpUtils.sendRequest(url+"?"+dataParams.substring(0,dataParams.length()-1),"",HttpUtils.METHOD_GET);
            }else{

                if(data==null){
                    data = "";
                }else{
                    String dataParams = "";

                    Set<String> dataSet = new HashSet<String>();

                    if(!"".equals(data) && data!= null){
                        JSONObject paramObject = JSON.parseObject(data);

                        dataSet = paramObject.keySet();

                        if(dataSet.size() > 0 ){
                            for (String key : dataSet) {
                                dataParams += "&"+ key + "=" + paramObject.getString(key);
                            }
                        }

                        dataParams = dataParams.substring(1);
                    }
                    data = dataParams;

                }

                con = httpUtils.sendRequest(url, data, method);
            }

            result = httpUtils.getResponse(con);

            if(serverName.equals("login")){//登录接口进行特殊处理，需要添加一个token返回值
                if(result.contains("-1")){
                }else{
                    Message message = JSON.parseObject(result,Message.class);
                    if(message.getCode().equals("0")){//用户登录成功
                        message.getContent().setToken(TokenUtils.token(message.getContent().getE_mail()));
                        result = JSONObject.toJSONString(message);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("请求服务端接口发生异常，异常如下：",e);

            resp.setContentType("text/json;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().println(" {\"code\":\"-1\",\"message\":\"server error,please try later\"}");
            return;
        }

        resp.setContentType("text/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().println(result);

        return;
    }

    @Override
    public void init() throws ServletException {
        Properties props = new Properties();
        InputStream isp = Inter.class.getClassLoader().getResourceAsStream("config.properties");
        try {
            props.load(new InputStreamReader(isp, "utf-8"));
        } catch (IOException e) {
            logger.error("Servlet初始化服务器参数错误，异常如下：", e);
        }
        serverUrl =  props.getProperty("serverUrl");
        appID =  props.getProperty("appID");
        token =  props.getProperty("token");
    }
}


