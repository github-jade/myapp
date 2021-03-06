package com.web.myapp.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

public class HTTPUtils {
	private static Logger log = Logger.getLogger(HTTPUtils.class);
	public static final String TYPE_XML = "text/xml";
	public static final String TYPE_JSON = "application/json";

	public static String postHttp(String url, String[] paramNames, String[] paramValues) {
		String responseMsg = "";
		// 1.构造HttpClient的实例
		HttpClient httpClient = new HttpClient();
		httpClient.getParams().setContentCharset("GBK");
		// 2.构造PostMethod的实例
		PostMethod postMethod = new PostMethod(url);
		// 3.把参数值放入到PostMethod对象中
		// 方式1：
//		NameValuePair[] data = {new NameValuePair("param1", param1), new NameValuePair("param2", param2)}; 
//		postMethod.setRequestBody(data);
		// 方式2：
		for (int index = 0; index < paramNames.length; index++) {
			postMethod.addParameter(paramNames[index], paramValues[index]);
		}
		try {
			// 4.执行postMethod,调用http接口
			httpClient.executeMethod(postMethod);// 200
			// 5.读取内容
			responseMsg = postMethod.getResponseBodyAsString().trim();
			// 6.处理返回的内容
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 7.释放连接
			postMethod.releaseConnection();
		}
		return responseMsg;
	}

	public static String httpPost(File file, String url) throws Exception {
		String result = null;
		URL urlObj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		con.setRequestMethod("POST"); // 以Post方式提交表单，默认get方式
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false); // post方式不能使用缓存
		// 设置请求头信息
		con.setRequestProperty("Connection", "Keep-Alive");
		con.setRequestProperty("Charset", "UTF-8");
		// 设置边界
		String BOUNDARY = "----------" + System.currentTimeMillis();
		con.setRequestProperty("Content-Type", "multipart/form-data; boundary="
				+ BOUNDARY);
		// 请求正文信息
		// 第一部分：
		StringBuilder sb = new StringBuilder();
		sb.append("--"); // 必须多两道线
		sb.append(BOUNDARY);
		sb.append("\r\n");
		sb.append("Content-Disposition: form-data;name=\"media\";filename=\""
				+ file.getName() + "\"\r\n");
		sb.append("Content-Type:application/octet-stream\r\n\r\n");
		byte[] head = sb.toString().getBytes("utf-8");
		// 获得输出流
		OutputStream out = new DataOutputStream(con.getOutputStream());
		// 输出表头
		out.write(head);
		// 文件正文部分

		// 把文件已流文件的方式 推入到url中
		DataInputStream in = new DataInputStream(new FileInputStream(file));
		int bytes = 0;
		byte[] bufferOut = new byte[1024];
		while ((bytes = in.read(bufferOut)) != -1) {
			out.write(bufferOut, 0, bytes);
		}
		in.close();
		// 结尾部分
		byte[] foot = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("utf-8");// 定义最后数据分隔线
		out.write(foot);
		out.flush();
		out.close();
		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = null;
		try {
			// 定义BufferedReader输入流来读取URL的响应
			reader = new BufferedReader(new InputStreamReader(con
					.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				// log.info(line);
				buffer.append(line);
			}
			if (result == null) {
				result = buffer.toString();
			}
		} catch (IOException e) {
			log.info("发送POST请求出现异常！" + e.getMessage());
			e.printStackTrace();
			throw new IOException("数据读取异常");
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return result;
	}

	/**  
	* 获取IP地址 
	* @param request
	* @return String 
	*/
	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		if (ip.contains(",") && ip.contains(":")) {
			ip = ip.substring(0, ip.indexOf(",")).trim();
		} else if (ip.contains(":") && !ip.contains(",")) {
			ip = ip.substring(0, ip.indexOf(":")).trim();
		}
		return ip;
	}

	/**
	 * 向指定URL发送GET方法的请求
	 * 
	 * @param url
	 *            发送请求的URL
	 * @param param
	 *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return URL 所代表远程资源的响应结果
	 */
	public static String sendGet(String url) {
		String result = "";
		BufferedReader in = null;
		try {

			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection connection = realUrl.openConnection();
			// 设置通用的请求属性
			connection.setRequestProperty("accept", "*/*");
			connection.setRequestProperty("connection", "Keep-Alive");
			connection.setRequestProperty("user-agent", 
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 建立实际的连接
			connection.connect();
			// 获取所有响应头字段
//			Map<String, List<String>> map = connection.getHeaderFields(); 
			// 遍历所有的响应头字段 
//			for (String key : map.keySet()) { 
//				log.info(key + "--->" + map.get(key));
//			}
			// 定义 BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
			result = new String(result.getBytes(), "utf-8");
		} catch (Exception e) {
			log.info("发送GET请求出现异常！" + e.getMessage());
			e.printStackTrace();
		} finally {// 使用finally块来关闭输入流
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * 向指定URL发送GET方法的请求
	 * 
	 * @param url
	 *            发送请求的URL
	 * @param path
	 *            文件保存的地址
	 * @return URL 所代表远程资源的响应结果
	 */
	public static String sendGetOfFile(String url, String path) {
		String result = "";
		BufferedReader in = null;
		DataInputStream dataInputStream = null;
		File file = new File(path);
		OutputStream outputStream = null;
		if (!file.exists()) {
			file.mkdirs();
		}
		try {

			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection connection = realUrl.openConnection();
			// 设置通用的请求属性
			connection.setRequestProperty("accept", "*/*");
			connection.setRequestProperty("connection", "Keep-Alive");
			connection.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 建立实际的连接
			connection.connect();
			// 获取所有响应头字段
			Map<String, List<String>> map = connection.getHeaderFields();
			// 遍历所有的响应头字段
//			for (String key : map.keySet()) {
//				log.info(key + "-->" + map.get(key));
//			}
			String contentType = map.get("Content-Type").get(0);
			if (contentType.indexOf("text") >= 0) {
				in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line;
				while ((line = in.readLine()) != null) {
					result += line;
				}
				return result;
			}
			List<String> contentDisp = map.get("Content-disposition");
			if (contentDisp == null) {
				result = StringUtils.CreateNoncestr() + ".jpg";
			} else {
				String disposition = map.get("Content-disposition").get(0);
				Pattern p = Pattern.compile("\"(.*)\"");
				Matcher m = p.matcher(disposition);
				if (m.find()) {
					result = m.group(1);
				}
			}
			// 定义 BufferedReader输入流来读取URL的响应
			// 1K的数据缓冲
			dataInputStream = new DataInputStream(connection.getInputStream());
			outputStream = new FileOutputStream(path + "/" + result);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = dataInputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, length);
			}
		} catch (Exception e) {
			log.info("发送GET请求出现异常！" + e.getMessage());
			e.printStackTrace();
		} finally {// 使用finally块来关闭输入流
			try {
				if (outputStream != null) {
					outputStream.close();
				}
				if (in != null) {
					in.close();
				}
				if (dataInputStream != null) {
					dataInputStream.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * 向指定 URL 发送POST方法的请求
	 * 
	 * @param url
	 *            发送请求的 URL
	 * @param param
	 *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return 所代表远程资源的响应结果
	 */
	public static String sendPost(String url, String param) {
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			InputStream newin = conn.getInputStream();
			int l = newin.available();
			byte[] b = new byte[l];
			while ((newin.read(b)) > 0) {
				String s = new String(b, "utf-8");
				result += s;
			}
		} catch (Exception e) {
			log.info("发送 POST 请求出现异常！" + e.getMessage());
			e.printStackTrace();
		} finally {// 使用finally块来关闭输入流
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * 向指定 URL 发送POST方法的请求
	 * 
	 * @param url
	 *            发送请求的 URL
	 * @param param
	 *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return 所代表远程资源的响应结果
	 */
	public static String sendPostWithHeader(String url, String param, Map<String, Object> header) {
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			Iterator<String> ite = header.keySet().iterator();
			for (; ite.hasNext();) {
				String key = (String) ite.next();
				String value = (String) header.get(key);
				conn.setRequestProperty(key, value);
			}
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			
//			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//			String line; 
//			while ((line = in.readLine()) != null) { 
//				result += line; 
//			} 
//			result = new String(result.getBytes(),"utf-8");
			
			InputStream newin = conn.getInputStream();
			int l = newin.available();
			byte[] b = new byte[l];
			while ((newin.read(b)) > 0) {
				String s = new String(b, "utf-8");
				result += s;
			}
		} catch (Exception e) {
			log.info("发送 POST 请求出现异常！" + e.getMessage());
			e.printStackTrace();
		} finally {// 使用finally块来关闭输出流、输入流
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * 向指定URL发送GET方法的请求
	 * 
	 * @param url
	 *            发送请求的URL
	 * @param param
	 *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return URL 所代表远程资源的响应结果
	 */
	public static String sendGetWithHeader(String url, Map<String, Object> header) {
		String result = "";
		BufferedReader in = null;
		try {

			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection connection = realUrl.openConnection();
			// 设置通用的请求属性
			Iterator<String> ite = header.keySet().iterator();
			for (; ite.hasNext();) {
				String key = (String) ite.next();
				String value = (String) header.get(key);
				connection.setRequestProperty(key, value);
			}
			connection.setRequestProperty("accept", "*/*");
			connection.setRequestProperty("connection", "Keep-Alive");
			connection.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 建立实际的连接
			connection.connect();
			// 获取所有响应头字段
//			Map<String, List<String>> map = connection.getHeaderFields(); 
			// 遍历所有的响应头字段 
//			for (String key : map.keySet()) { 
//				log.info(key + "--->" + map.get(key));
//			}
			// 定义 BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(connection
					.getInputStream()));
			InputStream newin = connection.getInputStream();
			int l = newin.available();
			byte[] b = new byte[l];
			while ((newin.read(b)) > 0) {
				String s = new String(b, "utf-8");
				result += s;
			}
//			result = new String(result.getBytes(),"utf-8");
		} catch (Exception e) {
			log.info("发送GET请求出现异常！" + e.getMessage());
			e.printStackTrace();
		}
		// 使用finally块来关闭输入流
		finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return result;
	}

	public static String httpPostFile(String path, String filePath) {
		File file = null;
		try {
			file = new File(filePath);
		} catch (Exception e) {
			log.error(e.getMessage());
			return "fail";
		}
		PostMethod filePost = new PostMethod(path);
		HttpClient client = new HttpClient();
		String info = "";
		try {
			Part[] parts = { new FilePart("media", file) };
			filePost.setRequestEntity(new MultipartRequestEntity(parts,
					filePost.getParams()));

			client.getHttpConnectionManager().getParams().setConnectionTimeout(
					5000);
			int status = client.executeMethod(filePost);
			if (status == HttpStatus.SC_OK) {
				log.info("上传成功");
			} else {
				log.info("上传失败");
			}
			info = new String(filePost.getResponseBody(), "utf-8");
			log.info("接口返回信息：" + info);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			filePost.releaseConnection();
		}
		return info;
	}

	public static String httpPost(String path,String type,String params){
    	
        PostMethod post = new PostMethod(path);
        HttpClient client = new HttpClient();
        String info = "";
        try {
        	RequestEntity requestEntity = new StringRequestEntity(params,type,"UTF-8");
        	post.setRequestEntity(requestEntity);
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            int status = client.executeMethod(post);
            if (status == HttpStatus.SC_OK) {
            	log.info("上传成功");
            } else {
            	log.info("上传失败");
            }
            info = new String(post.getResponseBody(), "utf-8");
            log.info("接口返回信息：" + info);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
        	post.releaseConnection();
        }
        return info;
    }

	public static String httpPost(String path, String params) {

		PostMethod post = new PostMethod(path);
		HttpClient client = new HttpClient();
		String info = "";
		try {
			RequestEntity requestEntity = new StringRequestEntity(params,
					"text/xml", "UTF-8");
			post.setRequestEntity(requestEntity);
			client.getHttpConnectionManager().getParams().setConnectionTimeout(
					5000);
			int status = client.executeMethod(post);
			if (status == HttpStatus.SC_OK) {
				log.info("上传成功");
			} else {
				log.info("上传失败");
			}
			info = new String(post.getResponseBody(), "utf-8");
			log.info("接口返回信息：" + info);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			post.releaseConnection();
		}
		return info;
	}

	public static String httpPost(String path, String params,
			Map<String, String> header) {

		PostMethod post = new PostMethod(path);
		HttpClient client = new HttpClient();
		String info = "";
		try {
			RequestEntity requestEntity = new StringRequestEntity(params);
			post.setRequestEntity(requestEntity);
			if (header != null && header.size() > 0) {
				List<Header> headers = new ArrayList<Header>();
				for (Map.Entry<String, String> entry : header.entrySet()) {
					
					headers.add(new Header(entry.getKey(),entry.getValue()));
				}
				
				client.getHostConfiguration().getParams().setParameter(
						"http.default-headers", headers);
			}
			post.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,  
					new DefaultHttpMethodRetryHandler());  

			int status = client.executeMethod(post);
			if (status == HttpStatus.SC_OK) {
				log.info("上传成功");
			} else {
				log.info("上传失败");
			}
			info = new String(post.getResponseBody(), "utf-8");
			log.info("接口返回信息：" + info);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			post.releaseConnection();
		}
		return info;
	}
	
	/** 
    * 发送HttpPost请求 
    * @param strURL 服务地址 
    * @param params 请求参数,格式如: "{\"id\":\"12345\"}";其中属性名必须带双引号
    * @return String json字符串
    */
    public static String sendPostWithJson(String strURL, String params) {  
        try {  
            URL url = new URL(strURL); // 创建连接  
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();  
            connection.setDoOutput(true);  
            connection.setDoInput(true);  
            connection.setUseCaches(false);  
            connection.setInstanceFollowRedirects(true);  
            connection.setRequestMethod("POST"); // 设置请求方式  
            connection.setRequestProperty("Accept", "application/json"); // 设置接收数据的格式  
            connection.setRequestProperty("Content-Type", "application/json"); // 设置发送数据的格式  
            connection.connect();  
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8"); // utf-8编码  
            out.append(params);  
            out.flush();  
            out.close();  
            // 读取响应  
            int length = (int) connection.getContentLength(); // 获取长度  
            InputStream is = connection.getInputStream();  
            if (length != -1) {  
                byte[] data = new byte[length];  
                byte[] temp = new byte[512];  
                int readLen = 0;  
                int destPos = 0;  
                while ((readLen = is.read(temp)) > 0) {  
                    System.arraycopy(temp, 0, data, destPos, readLen);  
                    destPos += readLen;  
                }  
                return new String(data, "UTF-8"); // utf-8编码  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
        return "error"; // 自定义错误信息  
    }
    
    private static class NullHostnameVerifier implements HostnameVerifier {
	    public boolean verify(String hostname, SSLSession session) {
	        return true;
	    }
	}
    private static class TrustAnyTrustManager implements X509TrustManager {
    	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
    			throws CertificateException {
    	}
    	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
    			throws CertificateException {
    	}
    	public X509Certificate[] getAcceptedIssuers() {
    		return null;
    	}
    }
    
    public static String postClient(String requestUrl, String requestMethod, 
    		String outputStr) {
		StringBuffer buffer = new StringBuffer();
		try {
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new TrustAnyTrustManager() };
			SSLContext sslContext = SSLContext.getInstance("TLSv1", "SunJSSE");
			sslContext.init(null, tm, new SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();

			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url
					.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);
			httpUrlConn.setHostnameVerifier(new NullHostnameVerifier());
			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);
			if ("GET".equalsIgnoreCase(requestMethod))
				httpUrlConn.connect();

			// 当有数据需要提交时
			if (null != outputStr && !outputStr.equals("")) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes("UTF-8"));
				outputStream.close();
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream, "utf-8");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String str = bufferedReader.readLine();
			while (str != null) {
				buffer.append(str);
			}
			bufferedReader.close();
			inputStreamReader.close();

			// 释放资源
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
		} catch (ConnectException ce) {
			ce.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buffer.toString();
	}
    
    public static String postClient(String requestUrl, File file, 
    		String outputStr, String type) {
		StringBuffer buffer = new StringBuffer();
		try {
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new TrustAnyTrustManager() };
			SSLContext sslContext = SSLContext.getInstance("TLSv1", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();

			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);
			httpUrlConn.setHostnameVerifier(new NullHostnameVerifier());
			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod("POST");
			StringBuilder sb = new StringBuilder();
			String BOUNDARY = "----------" + System.currentTimeMillis();  
			if("image".equals(type)){
				// 设置边界  
		        httpUrlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary="+ BOUNDARY);  
		        // 请求正文信息  
		        // 第一部分：  
		        sb.append("--");// 必须多两道线  
		        sb.append(BOUNDARY);  
		        sb.append("\r\n");  
		        sb.append("Content-Disposition: form-data;name=\"media\";filename=\"" + file.getName() + "\"\r\n");  
		        sb.append("Content-Type:application/octet-stream\r\n\r\n");  
			}
			OutputStream outputStream = httpUrlConn.getOutputStream();
	        outputStream.write(sb.toString().getBytes("utf-8"));
			// 当有数据需要提交时
			if (null != outputStr && !outputStr.equals("")) {
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes("UTF-8"));
			}
			byte[] foot = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("utf-8");// 定义最后数据分隔线  
			outputStream.write(foot);  
			outputStream.flush();  
			outputStream.close();
			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream, "utf-8");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}
			bufferedReader.close();
			inputStreamReader.close();

			// 释放资源
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
		} catch (ConnectException ce) {
			ce.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buffer.toString();
	}
	
	public static void filePostClient(String requestUrl, String requestMethod,
			String outputStr, String savaFilePath) {
		try {
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new TrustAnyTrustManager() };
			SSLContext sslContext = SSLContext.getInstance("TLSv1", "SunJSSE");
			sslContext.init(null, tm, new SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();

			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url
					.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);
			httpUrlConn.setHostnameVerifier(new NullHostnameVerifier());
			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);

			if ("GET".equalsIgnoreCase(requestMethod))
				httpUrlConn.connect();

			// 当有数据需要提交时
			if (null != outputStr && !outputStr.equals("")) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes("UTF-8"));
				outputStream.close();
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			OutputStream  outputStream = new FileOutputStream(savaFilePath);

			int c = inputStream.read();
            while(c != -1) {
            	outputStream.write(c);
            }

			// 释放资源
            outputStream.flush();
			inputStream.close();
			outputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
		} catch (ConnectException ce) {
			ce.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
