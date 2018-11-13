# -*- coding: utf-8 -*-
from twisted.enterprise import adbapi
import MySQLdb
import MySQLdb.cursors
import time
import uuid
from scrapy.crawler import Settings as settings
# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://doc.scrapy.org/en/latest/topics/item-pipeline.html

class NewsspiderPipeline(object):

    def __init__(self):
        dbargs = dict(
            host='120.79.241.196',
            db='news',
            user='root',  # replace with you user name
            passwd='60002314',  # replace with you password
            charset='utf8',
            cursorclass=MySQLdb.cursors.DictCursor,
            use_unicode=True,
        )
        self.dbpool = adbapi.ConnectionPool('MySQLdb', **dbargs)

    def process_item(self, item, spider):
        res = self.dbpool.runInteraction(self.insert_into_table, item)
        return item

    def insert_into_table(self, conn, item):
        try:
            filename = str(uuid.uuid1()) + str(time.time()) + ".html"
            # print filename
            detail = item['detail']
            item['detail'] = filename
            conn.execute('insert into news(title,author,time,detail,type) values(%s,%s,%s,%s,%s)', (item['title'], item['author'], item['time'], item['detail'], item['type']))
            self.save(item['title'], item['author'], item['time'], detail, filename)
        except BaseException as e:
            # print e
            print '插入失败'

    # 保存为html文件
    def save(self,title, author, date, detail, filename):
        # 保存为html
        str = "<!DOCTYPE html>" + \
            "<html>" + \
            "" + \
            "<head>" + \
            "" + \
            "    <meta charset=\"utf-8\">" + \
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + \
            "    <title>" + title + "</title>" + \
            "    <link href=\"css/bootstrap.min.css?v=3.3.5\" rel=\"stylesheet\">" + \
            "    <link href=\"css/font-awesome.min.css?v=4.4.0\" rel=\"stylesheet\">" + \
            "    <link href=\"css/animate.min.css\" rel=\"stylesheet\">" + \
            "    <link href=\"css/style.min.css?v=4.0.0\" rel=\"stylesheet\"><base target=\"_blank\">" + \
            "</head>" + \
            "<body class=\"gray-bg\">" + \
            "    <div class=\"wrapper wrapper-content  animated fadeInRight article\" style=\"padding-top:0px;\">" + \
            "        <div class=\"row\">" + \
            "            <div class=\"col-lg-10 col-sm-12 col-lg-offset-1 \"  style=\"padding:5px 5px 5px 5px;\">" + \
            "                <div class=\"ibox\">" + \
            "                    <div class=\"ibox-content\" style=\"padding:15px 15px 15px 15px;\" >" + \
            "                       " + \
            "                        <div class=\"text-center article-title\">" + \
            "                            <h1 style=\"font-size:28px;\">" + title + " </h1>" + \
            "                        </div> " + \
            "            <div style=\"height:40px;width:100%;\" >" + \
            "               <div class=\"pull-right\">" + \
            "                  <button class=\"btn btn-white btn-xs\" type=\"button\">发布人：" + author + "</button>" + \
            "                   <button class=\"btn btn-white btn-xs\" type=\"button\">时间：" + date + "</button>" + \
            "               </div>" + \
            "            </div>" + detail + "</div>" + \
            "                </div>" + \
            "            </div>" + \
            "        </div>" + \
            "    </div>" + \
            "    <script src=\"js/jquery.min.js?v=2.1.4\"></script>" +  \
            "    <script src=\"js/bootstrap.min.js?v=3.3.5\"></script>" + \
            "    <script src=\"js/content.min.js?v=1.0.0\"></script>" + \
            "</body>" + \
            "" + \
            "</html>";

        fw = open("/usr/local/tomcat/webapps/news/htmls/"+filename, "w")
        fw.write(str)
        fw.close()
        pass
