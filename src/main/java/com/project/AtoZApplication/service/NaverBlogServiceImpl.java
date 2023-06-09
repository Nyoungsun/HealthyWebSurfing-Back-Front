package com.project.AtoZApplication.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class NaverBlogServiceImpl implements NaverBlogService {

    @Value("${X_Naver_Client_Id}")
    private String X_Naver_Client_Id;
    @Value("${X_Naver_Client_Secret}")
    private String X_Naver_Client_Secret;

    @Value("${X_NCP_APIGW_API_KEY_ID}")
    private String X_NCP_APIGW_API_KEY_ID;
    @Value("${X_NCP_APIGW_API_KEY}")
    private String X_NCP_APIGW_API_KEY;

    public JSONObject searchNaverBlog(String query, int start) {
        System.out.println("검색어: " + query + ", start: " + start);
        RestTemplate restTemplate = new RestTemplate();
        String apiURL = "https://openapi.naver.com/v1/search/blog?query=" + query + "&display=" + 10 + "&start=" + start + "&sort=sim";

        HttpHeaders headers = new HttpHeaders(); //요청 헤더
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Naver-Client-Id", X_Naver_Client_Id);
        headers.set("X-Naver-Client-Secret", X_Naver_Client_Secret);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(headers);
        ResponseEntity<String> responseBody = restTemplate.exchange(apiURL, HttpMethod.GET, request, String.class);

        JSONObject responseJSON = null;
        if (responseBody.getStatusCode().is2xxSuccessful()) {
            String responseStr = responseBody.getBody();
            JSONParser parser = new JSONParser();
            try {
                responseJSON = (JSONObject) parser.parse(responseStr); //JSONObject로 변환

            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        return responseJSON;
    }

    public List<String> crawlingNaverBlog(JSONObject responseBody) {
        JSONArray items = (JSONArray) responseBody.get("items");
        List<String> contentsList = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<Callable<String>> tasks = new ArrayList<>();

        long startTime = System.currentTimeMillis(); // 작업 시작 시간 측정

        for (int i = 0; i < items.size(); i++) {
            JSONObject element = (JSONObject) items.get(i);
            String url = (String) element.get("link");

            Callable<String> task = () -> {

                try {
                    Document document = Jsoup.connect(url).get();
                    Elements iframes = document.select("iframe#mainFrame");
                    String src = iframes.attr("src");
                    String realUrl = "http://blog.naver.com" + src;

                    Document realDocument = Jsoup.connect(realUrl).get();
                    Elements blogContent = realDocument.select("div.se-component.se-text.se-l-default");

                    if (blogContent.isEmpty()) {
                        blogContent = realDocument.select("div.post-view");
                    }

                    String content = blogContent.text();
                    content = content.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣0-9,. ]", ""); //한글, 숫자, 마침표, 쉼표를 제외한 외국어, 특수문자 제거
                    content = content.replaceAll(" ", ""); //모든 공백 제거

                    if (content.length() > 1000) {
                        String substring1 = content.substring(0, 300); // 앞에서부터 300자
                        String substring2 = content.substring(content.length() / 2 - 200, content.length() / 2 + 200); // 가운데 400자
                        String substring3 = content.substring(content.length() - 300); // 뒤에서부터 300자

                        content = substring1 + substring2 + substring3; //1000자
                    }
                    return content;

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            tasks.add(task);
        }
        try {
            List<Future<String>> futures = executorService.invokeAll(tasks);
            for (Future<String> future : futures) {
                String content = future.get();
                contentsList.add(content);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
        long endTime = System.currentTimeMillis(); // 전체 작업 종료 시간 측정
        long executionTime = endTime - startTime; // 전체 작업 실행 시간 계산

        return contentsList;
    }

//    public List<String> crawlingNaverBlog(JSONObject responseBody) {
//        JSONArray items = (JSONArray) responseBody.get("items");
//        List<String> contentsList = new ArrayList<>();
//
//        long startTime = System.currentTimeMillis(); // 작업 시작 시간 측정
//
//        for (int i = 0; i < items.size(); i++) {
//            JSONObject element = (JSONObject) items.get(i);
//            String url = (String) element.get("link");
//
//
//            try {
//                Document document = Jsoup.connect(url).get();
//                Elements iframes = document.select("iframe#mainFrame");
//                String src = iframes.attr("src");
//                String realUrl = "http://blog.naver.com" + src;
//
//                Document realDocument = Jsoup.connect(realUrl).get();
//                Elements blogContent = realDocument.select("div.se-component.se-text.se-l-default");
//
//                if (blogContent.isEmpty()) {
//                    blogContent = realDocument.select("div.post-view");
//                }
//
//                String content = blogContent.text();
//
//                content = content.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣0-9,. ]", "");
//                content = content.replaceAll("\\s+", " ");
//                content = content.trim();
//
//                if (content.length() > 1000) {
//                    content = content.substring(0, 1000);
//                }
//                contentsList.add(content);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        long endTime = System.currentTimeMillis(); // 작업 종료 시간 측정
//        long executionTime = endTime - startTime; // 작업 실행 시간 계산
//        System.out.println("Task executed in " + executionTime + " ms");
//
//        return contentsList;
//    }

    @Override
    public JSONArray clovaSentiment(List<String> contetnsList) {
        JSONArray jsonArray = new JSONArray();

        String text;
        for (int i = 0; i < contetnsList.size(); i++) {
            if (contetnsList.get(i).isEmpty()) {
                text = "중립";
            } else {
                text = contetnsList.get(i);
            }

            String apiURL = "https://naveropenapi.apigw.ntruss.com/sentiment-analysis/v1/analyze";

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders(); //요청 헤더
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-NCP-APIGW-API-KEY-ID", X_NCP_APIGW_API_KEY_ID);
            headers.set("X-NCP-APIGW-API-KEY", X_NCP_APIGW_API_KEY);

            Map<String, String> content = new HashMap<>(); //요청 바디
            content.put("content", text);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(content, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiURL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();

                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class); //responsBody에서 document필드만 추출하기 위해 파싱
                    Map<String, Object> document = (Map<String, Object>) responseMap.get("document");  //document필드만 추출
                    String sentiment = (String) document.get("sentiment");                             //document필드의 sentiment값 추출 (감정분석 결과)

                    Map<String, Object> confidence = (Map<String, Object>) document.get("confidence"); //document필드의 confidence필드 추출(neutral, positive, negative)

                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("sentiment", sentiment);
                    jsonObj.put("confidence", confidence);

                    jsonArray.add(jsonObj);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }//if
        }//for
        return jsonArray;
    }

    @Override
    public ResponseEntity<String> mySentiment(String text) {
        String apiURL = "https://naveropenapi.apigw.ntruss.com/sentiment-analysis/v1/analyze";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders(); //요청 헤더
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-NCP-APIGW-API-KEY-ID", X_NCP_APIGW_API_KEY_ID);
        headers.set("X-NCP-APIGW-API-KEY", X_NCP_APIGW_API_KEY);

        Map<String, String> content = new HashMap<>(); //요청 바디
        content.put("content", text);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(content, headers);
        ResponseEntity<String> response = restTemplate.exchange(apiURL, HttpMethod.POST, request, String.class);

        return response;
    }
}



