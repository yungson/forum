package org.example.forum.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    private static final String REPLACEMENT = "***";
    private TrieNode rootNode = new TrieNode();

    @PostConstruct // 表示在实力创建之后执行初始化
    public void init(){
        try(
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt"); //需要try帮其关闭
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword =reader.readLine())!=null){
                this.addKeyword(keyword);
            }
        }catch (IOException e){
            logger.error("Loading sensitive words failed");
        }
    }

    private void addKeyword(String keyword){
        TrieNode tempNode = rootNode;
        for(int i=0;i<keyword.length();i++){
            char c = keyword.charAt(i);
            TrieNode childNode = tempNode.getChildNode(c);
            if (childNode == null){
                childNode = new TrieNode();
                tempNode.addChildNode(c, childNode);
            }
            tempNode = childNode;
            if (i==keyword.length()-1){
                tempNode.setKeywordEnd(true);
            }
        }
    }
    /**
     * @param
     *
     */
    public String filter(String text) {
        if(StringUtils.isBlank(text)) {return null;}
        TrieNode tempNode = rootNode;
        int left = 0;
        int right = 0;
        StringBuilder sb = new StringBuilder();
        while( left < text.length()){
            char c = text.charAt(right);
            // 跳过符号，处理情况比如： "加🌟微🌟信"
            if( isSymbol(c)) {
                if(tempNode == rootNode){ // 如果tempNode处于根结点， 说明开头是符号
                    sb.append(c); // 将此符号加入结果
                    left++; //匹配下一个字符
                }
                // 符号在中间就不将符号加入结果
                right++;
                continue;
            }
            tempNode = tempNode.getChildNode(c);
            if(tempNode == null){ // left-right这段不是敏感词
                sb.append(text.charAt(left));
                right = ++left; // 进入下一个位置
                tempNode = rootNode;
            }else if (tempNode.isKeywordEnd){
                // 发现敏感词
                sb.append(REPLACEMENT);
                left = ++right;
                tempNode = rootNode;
            }else {
                if (right>=text.length()-1) {
                    sb.append(text.charAt(left));
                    right = ++left;
                    tempNode = rootNode;
                } else {
                    right++;
                }
            }
        }
        // 将最后一批字符加入结果
//        sb.append(text.substring(left));
        return sb.toString();
    }

    private boolean isSymbol(Character c){
        // 0x2E80  ~ c>0x9FFF是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c<0x2E80 || c>0x9FFF);
    }

    private class TrieNode{ //类内也可以定义类
        private boolean isKeywordEnd = false;
        private Map<Character, TrieNode> children = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }
        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }
        public void addChildNode(Character c, TrieNode node){
            children.put(c, node);
        }
        public TrieNode getChildNode(Character c){
            return children.get(c);
        }
    }
}
