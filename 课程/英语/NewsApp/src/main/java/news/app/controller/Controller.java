package news.app.controller;

import news.app.model.News;
import news.app.service.NewsService;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.server.PathParam;
import java.util.List;

/**
 * Created by 14260 on 2018/10/30.
 */

@RestController
@EnableAutoConfiguration
@ComponentScan(basePackages = {"news.app.service"})
public class Controller {

    @Autowired
    NewsService newsService ;

    @RequestMapping("/list")
    List<News> loadByTypeAndPage(int type,int pageIndex, int pageSize){
        System.out.println(type);
        System.out.println(pageIndex);
        System.out.println(pageSize);
        return newsService.loadByTypeAndPage(type+"",pageIndex,pageSize);
    }

    @RequestMapping("/hello")
    String loadByTypeAndPage(){
        return "hello";
    }
}
