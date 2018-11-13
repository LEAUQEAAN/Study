package news.app.service;

import news.app.dao.NewsDao;
import news.app.model.News;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Created by 14260 on 2018/10/30.
 */
@Service
public class NewsService {

    @Autowired
    NewsDao newsDao ;

    public List<News> loadByTypeAndPage(String type, int pageIndex, int pageSize) {
       return newsDao.loadByTypeAndPage(type,pageIndex*pageSize-pageSize,pageSize);
    }
}
