// pages/detail/detail.js
const $vm = getApp()  
const WxParse = require('../../utils/wxParse/wxParse.js');
Page({

  /**
   * 页面的初始数据
   */
  data: {
    wxParseData: [],
  },

  /**
   * 生命周期函数--监听页面加载
   */
  init:function(options){
    var thats = this;
    
    wx.request({
      url: 'http://localhost:8080/loadByTitle', //仅为示例，并非真实的接口地址
      data: {
        title: options.title,
      },
      header: {
        'content-type': 'application/json' // 默认值
      },
      success: (res) => {
        console.log(res);
        var article = res.data.detail;
        WxParse.wxParse('html', article, this);
        thats.setData({
          article: res.data,
        });


      }, fail: (error) => {
        console.log(error)
        wx.showToast({
          title: '数据获取失败！',
          icon: 'none',
          duration: 2000
        })
      }
    })
  }
  ,
  onLoad: function (options) {
    this.init(options); 
 
   
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady: function () {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function () {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide: function () {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload: function () {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function () {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom: function () {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {

  }
})