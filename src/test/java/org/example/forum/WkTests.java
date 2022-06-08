package org.example.forum;

import java.io.IOException;

public class WkTests {
    public static void main(String[] args){
        String cmd = "/usr/local/bin/wkhtmltoimage --quality 75 https://www.nowcoder.com /Users/yongshengli/data/wkhtml/3.png";
        try {
            Process p = Runtime.getRuntime().exec(cmd); // 只是将命令提交给操作系统，跟我们的主程序是异步的
            if (p.waitFor() == 0 ){
                System.out.println("completed!");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
