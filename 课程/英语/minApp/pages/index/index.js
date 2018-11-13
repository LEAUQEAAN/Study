//index.js
//获取应用实例
const app = getApp()

Page({
  data: {    
  }
  ,
  onLoad: function () {
   
  },
  onShow: function () { 
     
  },go:function(e){
    var type = e.currentTarget.dataset.id ;
    wx.navigateTo({
      url: '../news/news?type='+type,
    }); 
  } 
})