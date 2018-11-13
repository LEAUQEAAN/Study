# -*- coding: utf-8 -*-
import scrapy

from newsSpider.items import NewsspiderItem
import requests
import time
import sys

reload(sys)
sys.setdefaultencoding('utf8')


class NewsspiderSpider(scrapy.Spider):
    name = 'newsspider'
    allowed_domains = ['edu.cn']
    num = 1
    # 硕士研究生招生                           1
    # 推免生接收（含免费师范生攻读教育硕士）   2
    # 华东师范大学考点(3111)及考试             3
    # 博士研究生招生                           4
    # 招收港澳台籍和留学生通知与公告           5

    start_urls = ['https://yjszs.ecnu.edu.cn/system/sszsxx_list.asp?page=1',
                  'https://yjszs.ecnu.edu.cn/system/tjmszsxx_list.asp?page=1',
                  'https://yjszs.ecnu.edu.cn/system/hskdxx_list.asp?page=1',
                  'https://yjszs.ecnu.edu.cn/system/bszsxx_list.asp?page=1',
                  'https://yjszs.ecnu.edu.cn/system/gatwxx_list.asp?page=1',
                  ]

    def parse(self, response):
        site = response.xpath('//ul[@class="pagination"]/li/a/@href').extract()

        for url in site:
            if url != '#':
                # print url
                if url.find('sszsxx_list') != -1:
                    self.num = 1
                elif url.find('tjmszsxx_list') != -1:
                    self.num = 2
                elif url.find('hskdxx_list') != -1:
                    self.num = 3
                elif url.find('bszsxx_list') != -1:
                    self.num = 4
                elif url.find('gatwxx_list') != -1:
                    self.num = 5
                yield scrapy.Request('https://yjszs.ecnu.edu.cn/system/'+url, meta={'type':self.num}, callback=self.detail_parse)

        pass

    def detail_parse(self, response):
        site = response.xpath('//div[@class="col-md-9"]/div/div/div/div/ul/li/a/@href').extract()
        type = response.meta['type']
        for url in site:
            if url != '#':
                # print url
                yield scrapy.Request('https://yjszs.ecnu.edu.cn/system/'+url, meta={'type':type}, callback=self.news_parse)
        pass

    def news_parse(self, response):

        title = response.xpath('//div[@class="col-md-12"]/div/h1/center/font/text()').extract()[0]
        # print title[0].replace("\r", "").replace("\n", "")
        info = response.xpath('//div[@class="col-md-12"]/div/h5/center/text()').extract()
        its = info[0].split("|");
        type = response.meta['type']

        author = its[0].replace(u"\u4f5c\u8005\uff1a", "")
        # print author
        date = its[1].replace(u" \u53d1\u8868\u65f6\u95f4\uff1a", "")
        # print time
        detail = response.xpath('//pre[@class="preclass"]').extract()[0].replace(u'\xa0', u'').replace("pre", "div")
        # print detail
        itm = NewsspiderItem()
        itm['title'] = title
        itm['author'] = author
        itm['time'] = date
        itm['detail'] = detail
        itm['type'] = type
        yield itm

