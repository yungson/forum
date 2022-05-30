package org.example.forum.controller;

import org.example.forum.service.AlphaService;
import org.example.forum.util.ForumUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Controller
@RequestMapping("/alpha")
public class AlphaController {

    @Autowired
    private AlphaService alphaService;

    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello(){
        return "Hello";
    }

    @RequestMapping("/data")
    @ResponseBody
    public String getData() {
        return alphaService.find();
    }


    // 在Spring MVC 下如何获取request, response对象（复杂的没有经过spring封装的底层方式）
    @RequestMapping("/http")
    public void http(HttpServletRequest request, HttpServletResponse response){
        //获取请求数据
        //请求行
        System.out.println(request.getMethod());
        System.out.println(request.getServletPath());
        Enumeration<String> enumeration = request.getHeaderNames();
        //请求头
        while(enumeration.hasMoreElements()) {
            String name = enumeration.nextElement();
            String value = request.getHeader(name);
            System.out.println(name+":"+value);
        }
        System.out.println(request.getParameter("code"));

        //返回响应数据
        response.setContentType("text/html;charset=utf-8");
        try (
                PrintWriter writer = response.getWriter(); //理论上打开writer之后需要在finally里面关闭。但是Java 7以后可以将其写在括号里。然后java会在执行完后自动生成finally然后close
        ){
            writer.write("<h1>Your Forum</h1>");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 简单的方式

    // 1.GET请求
    // /students?current=1&limits=20
    @RequestMapping(path = "/students", method = RequestMethod.GET) // 标识处理GET请求的students
    @ResponseBody
    public String getStudents(@RequestParam(name="current", required = false, defaultValue = "1") int current, //@RequestParam 获取请求里面的参数，并设置给对应的方法里面，还可设置是不是必要和默认值
                              @RequestParam(name="limit", required = false, defaultValue = "10") int limit){
        System.out.println("current = " + current + ", limit = " + limit);
        return "some students";
    }

    //还可以通过下面的方式，将参数编排到路径当中
    @RequestMapping(path = "/student/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(@PathVariable("id") int id){
        return " the student";
    }

    //2. POST请求
    //post请求主要用于传输数据，但其实get请求也可以传输信息，但是GET的话一般都放在路径里，明文传输不安全，且URL路径长度一般有限的，可传输的信息量有限
    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(String name, int age){
        System.out.println("name = " + name + ", age = " + age);
        return "success";
    }
    //配置完这个以及static/student.html以后可以打开http://localhost:8080/forum/html/student.html，
    // 填写数据并提交，看到浏览器显示success,并且路径变成了 http://localhost:8080/forum/alpha/student
    // 所以并不是直接访问/alpha/student. 而是通过静态资源生成的POST请求

    //3. 向浏览器响应动态的HTML数据

    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    //此时这里就不用写@ResponseBody了，因为默认就是HTML
    public ModelAndView getTeacher(){ //说明返回的是ModelAndView.
        ModelAndView mav = new ModelAndView();
        mav.addObject("name", "xiaoming");
        mav.addObject("age","30");
        mav.setViewName("/demo/view"); //setView是在设置模版，模版一般存放在resources/templates下面，设置的时候不用写templates, 和.html后缀
        return mav;
    }

    //另外一种方式，通过String而不是ModelAndView,这种方式更简单
    @RequestMapping(path = "/school", method = RequestMethod.GET)
    public String getSchool(Model model){ //dispatcherServlet会自动传一个model进来，让你设置属性值装数据进去
        model.addAttribute("name","Peking University");
        model.addAttribute("age", "125");

        return "/demo/view"; // 当返回的是String的时候，本质上是返回给dispatcherServlet，等同于说去访问这个路径
    }

    //4. 响应json数据(通常是在异步请求当中， 比如当你打开B站的注册页面，开始注册，输入牛客网，然后页面会即刻显示改昵称已占用。但是该页面并未刷新，)
    // 网页发送请求查询数据库判断昵称是否可用就是异步请求，这是一种局部验证的场景
    // json的价值：Java对象<---->json<---->JavaScript对象
    @RequestMapping(path = "/employee", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getEmp(){ //当dispatcherServlet看到你返回的是Map<String, Object>这样的类型的时候，会自动将其转换成json字符串发送给浏览器
        Map<String, Object> emp = new HashMap<>();
        emp.put("name","xiaoming");
        emp.put("age", 23);
        emp.put("salary", 8000);
        return emp;
    }

    @RequestMapping(path = "/employees", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> getEmps(){ //
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> emp = new HashMap<>();
        emp.put("name","xiaoming");
        emp.put("age", 23);
        emp.put("salary", 8000);
        list.add(emp);
        emp = new HashMap<>();
        emp.put("name","xiaohong");
        emp.put("age", 24);
        emp.put("salary", 10000);
        list.add(emp);
        return list;
    }
    //5. 还可以响应其他很多种数据

    //6.cookie示例

    @RequestMapping(path="/cookie/set", method =RequestMethod.GET )
    @ResponseBody
    public String setCookie(HttpServletResponse response){
        Cookie cookie = new Cookie("code", ForumUtil.generateUUID());
        cookie.setPath("/forum/alpha");
        cookie.setMaxAge(60*10);
        response.addCookie(cookie);
        return "set cookie";
    }

    // 其他请求就会带上这个cookie
    @RequestMapping(path="cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code){
        System.out.println("code = " + code);
        return "get cookie";
    }

    //session example
    @RequestMapping(path = "/session/set", method=RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session){
        session.setAttribute("id", 1);
        session.setAttribute("name", "Test");
        return "set session";
    }

    // 下次访问就可以获取对应请求中的sessionId,然后对应相应的session
    @RequestMapping(path = "/session/get", method=RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session){
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "get session";
    }
}


