$(function (){
    $("#uploadForm").submit(upload);
    // 当我点击Form的提交按钮触发表单的提交事件时，事件由upload函数来处理
});

function upload(){

    $.ajax({
        url: "http://upload-z1.qiniup.com",
        method: "post",
        processData: false, // 不要把表单内容转换为字符串
        contentType: false, // 不让jquery设置上传类型，浏览器会自动进行设置，文件是二进制数据，浏览器需要去加特定的字符串来识别边界
        data: new FormData($("#uploadForm")[0]), // $("#uploadForm")是jquery对象，本质是js数组，取[0]即可得到data
        success: function(data){ // 七牛云会直接返回json不需要解析
            if (data && data.code==0){
                // 更新头像URL
                $.post(
                    CONTEXT_PATH+"/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function (data){
                        data = $.parseJSON(data); //Forum服务器返回的数据是序列化的需要解析
                        if (data.code == 0){
                            window.location.reload();
                        }else{
                            alert(data.msg);
                        }
                    }
                )
            }else{
                alert("上传失败！");
            }
        }
    })

    return false;// 告诉js不需要执行action了，因为form里面我们没有定义action, 也就是说事件到此为止（因为我们已经在upload函数里将事件逻辑处理完毕），不再像正常的那样再去执行action
}