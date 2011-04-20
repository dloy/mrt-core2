
/*********************************************************************
    Copyright 2003 Regents of the University of California
    All rights reserved
*********************************************************************/

package org.cdlib.mrt.core;

import java.net.URL;
import org.cdlib.mrt.utility.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


import org.cdlib.mrt.core.Identifier;
import org.cdlib.mrt.formatter.FormatInfo;
import org.cdlib.mrt.utility.TException;
import org.cdlib.mrt.utility.LoggerInf;
import org.cdlib.mrt.utility.FileUtil;
import org.cdlib.mrt.utility.FixityTests;
import org.cdlib.mrt.utility.PropertiesUtil;
import org.cdlib.mrt.utility.StringUtil;
import org.cdlib.mrt.utility.TFileLogger;
import org.cdlib.mrt.utility.URLEncoder;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.Header;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.protocol.HTTP;



import java.io.File;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;


/**
 * AddClient for Storage
 * @author  loy
 */

public class FixityClient
{
    private static final String NAME = "FixityAddClient";
    private static final String MESSAGE = NAME + ": ";
    protected final static String NL = System.getProperty("line.separator");
    protected final static String FORMAT_NAME_POST = "t";
    protected final static String FORMAT_NAME_MULTIPART = "response-form";

    protected LoggerInf logger = null;

    public FixityClient(LoggerInf logger)
    {
        this.logger = logger;
    }

    public FixityClient()
    {
        this.logger = new TFileLogger(NAME, 5, 5);
    }

    public Properties add(
            String linkS,
            int timeout,
            int retry,
            String urlS,
            String source,
            String sizeS,
            String digestType,
            String digestValue,
            String context,
            String note,
            String formatTypeS)
        throws TException
    {
        URL link = null;
        URL url = null;
        long size = -1;
        MessageDigest messageDigest = null;
        FormatInfo format = null;

        try {
            if (StringUtil.isEmpty(linkS)) {
                throw new TException.INVALID_OR_MISSING_PARM("link required");
            }
            try {
                link = new URL(linkS);
            } catch (Exception ex) {
                throw new TException.INVALID_OR_MISSING_PARM("link invalid:" + linkS);
            }

            if (StringUtil.isEmpty(urlS)) {
                throw new TException.INVALID_OR_MISSING_PARM("url required");
            }
            try {
                url = new URL(urlS);
            } catch (Exception ex) {
                throw new TException.INVALID_OR_MISSING_PARM("url invalid:" + urlS);
            }

            if (StringUtil.isEmpty(source)) {
                throw new TException.INVALID_OR_MISSING_PARM("source required");
            }
            if (StringUtil.isEmpty(sizeS)) {
                throw new TException.INVALID_OR_MISSING_PARM("sizeS required");
            }

            try {
                size = Long.parseLong(sizeS);
            } catch (Exception ex) {
                throw new TException.INVALID_OR_MISSING_PARM("sizeS invalid:" + sizeS);
            }

            if (StringUtil.isEmpty(digestType)) {
                throw new TException.INVALID_OR_MISSING_PARM("digestType required");
            }
            if (StringUtil.isEmpty(digestValue)) {
                throw new TException.INVALID_OR_MISSING_PARM("digestVersion required");
            }
            try {
                messageDigest = new MessageDigest(digestValue, digestType);
            } catch (Exception ex) {
                throw new TException.INVALID_OR_MISSING_PARM("digest invalid:"
                        + " - digestType=" + digestType
                        + " - digestValue=" + digestValue
                        );
            }
            if (StringUtil.isEmpty(context)) {
                throw new TException.INVALID_OR_MISSING_PARM("context required");
            }
            if (StringUtil.isEmpty(formatTypeS)) {
                formatTypeS = "XML";
            }

            try {
                formatTypeS = formatTypeS.toLowerCase();
                format = FormatInfo.valueOf(formatTypeS);
            } catch (Exception ex) {
                throw new TException.INVALID_OR_MISSING_PARM("formatType invalid:" + formatTypeS);
            }
            if (!format.getForm().equals("state")) {
                throw new TException.INVALID_OR_MISSING_PARM("formatType not supported:" + formatTypeS);
            }
            log(MESSAGE + "addClient:"
                    + " - url=" + url.toString()
                    + " - source=" + source
                    + " - size=" + size
                    + " - messageDigest=" + messageDigest.toString()
                    + " - context=" + context
                    + " - note=" + note
                    + " - format=" + format.toString(),10);
            if (false) return null; //!!!!!!
            HttpResponse  response = sendAddMultipartRetry(
                link,
                timeout,
                retry,
                url,
                source,
                size,
                messageDigest,
                context,
                note,
                format);
            return processResponse(response);

        } catch (Exception ex) {
            log(MESSAGE + "Exception:" + ex, 3);
            log(MESSAGE + "Trace:" + StringUtil.stackTrace(ex), 10);
            throw new TException.GENERAL_EXCEPTION(ex);

        }

    }

     public HttpResponse sendAddMultipart(
                URL link,
                int timeout,
                URL url,
                String source,
                long size,
                MessageDigest messageDigest,
                String context,
                String note,
                FormatInfo format)
        throws TException
    {
        try {
            String addFixityURLS = link.toString() + "/add";
            URL addFixityURL = new URL(addFixityURLS);
            log(MESSAGE + "addCLient:"
                    + " - url=" + url.toString()
                    + " - source=" + source
                    + " - size=" + size
                    + " - messageDigest=" + messageDigest.toString()
                    + " - context=" + context
                    + " - note=" + note
                    + " - format=" + format.toString(),10);
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.socket.timeout", new Integer(timeout));
            params.setParameter("http.connection.timeout", new Integer(timeout));

            HttpClient httpclient = new DefaultHttpClient(params);
            HttpPost httppost = new HttpPost(addFixityURL.toString());
            MultipartEntity reqEntity = new MultipartEntity();
            if (url != null) {
                StringBody body = new StringBody(url.toString());
                reqEntity.addPart("url", body);
            }

            if (StringUtil.isNotEmpty(source)) {
                StringBody body = new StringBody(source);
                reqEntity.addPart("source", body);
            }
            if (size > 0) {
                StringBody body = new StringBody("" + size);
                reqEntity.addPart("size", body);
            }
            if (messageDigest != null) {
                String digestType = messageDigest.getJavaAlgorithm();
                String digestValue = messageDigest.getValue();
                StringBody body = new StringBody(digestType);
                reqEntity.addPart("digest-type", body);
                StringBody body2 = new StringBody(digestValue);
                reqEntity.addPart("digest-value", body2);
            }
            if (StringUtil.isNotEmpty(context)) {
                StringBody body = new StringBody(context);
                reqEntity.addPart("context", body);
            }
            if (StringUtil.isNotEmpty(note)) {
                StringBody body = new StringBody(note);
                reqEntity.addPart("note", body);
            }
            if (format != null) {
                String formatType = format.toString();
                StringBody body = new StringBody(formatType);
                reqEntity.addPart(FORMAT_NAME_MULTIPART, body);
            }

            httppost.setEntity(reqEntity);

            System.out.println("executing request " + httppost.getRequestLine());
            HttpResponse response = httpclient.execute(httppost);
            return response;

        } catch (Exception ex) {
            log(MESSAGE + "Exception:" + ex, 3);
            log(MESSAGE + "Trace:" + StringUtil.stackTrace(ex), 10);
            throw new TException.GENERAL_EXCEPTION(ex);

        }
    }

    /**
     * getObject with timeout and retry
     * @param requestURL build inputStream to this URL
     * @param timeout milliseconds for timeout
     * @param retry number of retry attemps
     * @return InputStream to URL service
     * @throws org.cdlib.mrt.utility.TException
     */
     public HttpResponse sendAddMultipartRetry(
            URL link,
            int timeout,
            int retry,
            URL url,
            String source,
            long size,
            MessageDigest messageDigest,
            String context,
            String note,
            FormatInfo format)
        throws TException
    {
        Exception exSave = null;
        for (int i=0; i < retry; i++) {
            try {
                HttpResponse  response = sendAddMultipart(
                    link,
                    timeout,
                    url,
                    source,
                    size,
                    messageDigest,
                    context,
                    note,
                    format);
                return response;

            } catch (Exception ex) {
                exSave = ex;
            }
        }
        if (exSave instanceof TException) {
            throw (TException) exSave;
        }
        throw new TException.EXTERNAL_SERVICE_UNAVAILABLE(
                MESSAGE + "sendAddMultipartRetry"
                + " Exception:" + exSave);
    }


    protected Properties processResponse(HttpResponse response)
        throws TException
    {
        try {
            Properties resultProp = new Properties();
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if ((statusCode >= 300) || (statusCode < 200)) {
                resultProp.setProperty("add.status", "" + statusCode);
            }
            HttpEntity resEntity = response.getEntity();
            String responseState = StringUtil.streamToString(resEntity.getContent(), "utf-8");
            if (StringUtil.isNotEmpty(responseState)) {
                resultProp.setProperty("add.state", responseState);
                System.out.println("mrt-response:" + responseState);
            }
            Header [] headers = response.getAllHeaders();
            for (Header header : headers) {
                resultProp.setProperty(
                        "header." + header.getName(),
                        header.getValue());
            }
            System.out.println(PropertiesUtil.dumpProperties("!!!!sendArchiveMultipart!!!!", resultProp, 100));

            System.out.println("----------------------------------------");
            System.out.println(response.getStatusLine());
            if (resEntity != null) {
                System.out.println("Response content length: " + resEntity.getContentLength());
                System.out.println("Chunked?: " + resEntity.isChunked());
            }
            if (resEntity != null) {
                resEntity.consumeContent();
            }
            return resultProp;

        } catch (Exception ex) {
            String msg = "Exception:" + StringUtil.stackTrace(ex);
            log(MESSAGE + "Exception:" + StringUtil.stackTrace(ex), 0);
            System.out.println(msg);
            ex.printStackTrace();
            throw new TException.GENERAL_EXCEPTION(ex);
        }

    }

    public Properties delete(
            String linkS,
            int timeout,
            int retry,
            String urlS,
            String formatTypeS)
        throws TException
    {
        URL link = null;
        URL url = null;
        long size = -1;
        MessageDigest messageDigest = null;
        FormatInfo format = null;

        try {
            if (StringUtil.isEmpty(linkS)) {
                throw new TException.INVALID_OR_MISSING_PARM("link required");
            }
            try {
                link = new URL(linkS);
            } catch (Exception ex) {
                throw new TException.INVALID_OR_MISSING_PARM("link invalid:" + linkS);
            }

            if (StringUtil.isEmpty(urlS)) {
                throw new TException.INVALID_OR_MISSING_PARM("url required");
            }
            try {
                url = new URL(urlS);
            } catch (Exception ex) {
                throw new TException.INVALID_OR_MISSING_PARM("url invalid:" + urlS);
            }
            if (StringUtil.isEmpty(formatTypeS)) {
                formatTypeS = "XML";
            }

            try {
                formatTypeS = formatTypeS.toLowerCase();
                format = FormatInfo.valueOf(formatTypeS);
            } catch (Exception ex) {
                throw new TException.INVALID_OR_MISSING_PARM("formatType invalid:" + formatTypeS);
            }
            if (!format.getForm().equals("state")) {
                throw new TException.INVALID_OR_MISSING_PARM("formatType not supported:" + formatTypeS);
            }
            log(MESSAGE + "deleteClient:"
                    + " - url=" + url.toString()
                    + " - format=" + format.toString(),10);
            if (false) return null; //!!!!!!
            HttpResponse  response = sendDeleteRetry(
                link,
                timeout,
                retry,
                url,
                format);
            return processResponse(response);

        } catch (Exception ex) {
            log(MESSAGE + "Exception:" + ex, 3);
            log(MESSAGE + "Trace:" + StringUtil.stackTrace(ex), 10);
            throw new TException.GENERAL_EXCEPTION(ex);

        }

    }

     public HttpResponse sendDelete(
                URL link,
                int timeout,
                URL url,
                FormatInfo format)
        throws TException
    {
        try {
            String deleteFixityURLS = link.toString() + "/item/"
                + URLEncoder.encode(url.toString(), "utf-8") + "?=" + format.toString();
            URL deleteFixityURL = new URL(deleteFixityURLS);
            log(MESSAGE + "sendDelete:"
                    + " - deleteFixityURL=" + deleteFixityURL.toString()
                    + " - format=" + format.toString(),10);
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.socket.timeout", new Integer(timeout));
            params.setParameter("http.connection.timeout", new Integer(timeout));

            HttpClient httpclient = new DefaultHttpClient(params);
            HttpDelete httpDelete = new HttpDelete(deleteFixityURL.toString());
            HttpResponse response = httpclient.execute(httpDelete);
            return response;

        } catch (Exception ex) {
            log(MESSAGE + "Exception:" + ex, 3);
            log(MESSAGE + "Trace:" + StringUtil.stackTrace(ex), 10);
            throw new TException.GENERAL_EXCEPTION(ex);

        }
    }

    /**
     * getObject with timeout and retry
     * @param requestURL build inputStream to this URL
     * @param timeout milliseconds for timeout
     * @param retry number of retry attemps
     * @return InputStream to URL service
     * @throws org.cdlib.mrt.utility.TException
     */
     public HttpResponse sendDeleteRetry(
            URL link,
            int timeout,
            int retry,
            URL url,
            FormatInfo format)
        throws TException
    {
        Exception exSave = null;
        for (int i=0; i < retry; i++) {
            try {
                HttpResponse  response = sendDelete(
                    link,
                    timeout,
                    url,
                    format);
                return response;

            } catch (Exception ex) {
                exSave = ex;
            }
        }
        if (exSave instanceof TException) {
            throw (TException) exSave;
        }
        throw new TException.EXTERNAL_SERVICE_UNAVAILABLE(
                MESSAGE + "sendDelete"
                + " Exception:" + exSave);
    }


    public void log(String msg, int lvl)
    {

        if (logger == null) System.out.println(msg);
        else logger.logMessage(msg, lvl);
    }

    protected void addNameValue(List <NameValuePair> nvps, String key, String value)
    {
        if (StringUtil.isEmpty(key)) return;
        if (StringUtil.isEmpty(value)) return;
        nvps.add(new BasicNameValuePair(key, value));
    }
}