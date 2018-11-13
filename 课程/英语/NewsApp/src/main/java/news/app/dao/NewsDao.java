package news.app.dao;

import news.app.model.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by 14260 on 2018/10/30.
 */
public interface NewsDao extends JpaRepository<News,String> {

    @Query(value = "select title,author,time,counts,detail,type from news where type = ?1 order by time desc limit ?2 , ?3 ",nativeQuery = true)
    List<News> loadByTypeAndPage(String type,int start,  int pageSize);

}
