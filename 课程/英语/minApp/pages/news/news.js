var app = getApp()
Page({
  data: {   
    listData:[],
    titles: ['硕士研究生招生', '推免生接收', '华东师范大学考点(3111)及考试', '博士研究生招生', '招收港澳台籍和留学生通知与公告'],
    start:1,
    type:1
  },
  onLoad: function (options) {   

    console.log(this.options.type)
    
    wx.setNavigationBarTitle({
      title: this.data.titles[this.options.type-1]
    })

    this.setData({
      type: this.options.type,
      start: 1,
      listData: [],
    })

    this.setDatas();

  }, 
  setDatas: function (){ 

    wx.request({
      url: 'https://www.leauqeaan.cn/news/list?type=1&pageIndex=1&pageSize=10', //仅为示例，并非真实的接口地址
      data: {
        type: this.data.type,
        pageIndex: this.data.start,
        pageSize: 10
      },
      header: {
        'content-type': 'application/json' // 默认值
      },
      success: (res) => {
        
        var s = this.data.start + 1; 
        var list = []
        if (this.data.listData != null){
           list = this.data.listData;
        }

        for(var i = 0 ; i < res.data.length ; i++){
          list.push(res.data[i]);
        }

        this.setData({ 
          listData:list,
          start: s
        })
      }, fail: (error) => {
        console.log(error)
        wx.showToast({
          title: '数据获取失败！',
          icon: 'none',
          duration: 2000
        })
      }
    })
  },
  onPullDownRefresh() { 
    setTimeout(() => {
      this.setData({
        start: 1,
        listData: []
      });
      this.setDatas();
      wx.stopPullDownRefresh();
    }, 1000);
  },
  onReachBottom() { 
    if (this.data.listData.length % 5 == 0) { 
      this.setDatas();
    } else {
      wx.showToast({
        title: '没有更多数据了！',
        icon:"none",
        duration:2000
      })
    }
  }
  
})  