$(function(){
    $("#topBtn").click(setTop);
    $("#creditBtn").click(setCredit);
    $("#deleteBtn").click(setDelete);
})
function like(button, entityType, entityId, entityUserId, postId){
    $.post(
        CONTEXT_PATH+"/like",
        {"entityType": entityType, "entityId": entityId, "entityUserId": entityUserId, "postId": postId},
        function(data){
            data = $.parseJSON(data);
            if(data.code==0){
                $(button).children("i").text(data.likeCount);
                $(button).children("b").text(data.likeStatus==1?"已赞":"赞");
            }else{
                alert(data.msg);
            }
        }
    );
}

function setTop(){
    $.post(
        CONTEXT_PATH+"/discuss/top",
        {"id": $("#postId").val()},
        function (data){
            data = $.parseJSON(data);
            if (data.code == 0){
                $("#topBtn").attr("disabled","disabled");
            } else{
                alert(data.msg);
            }
        }
    );
}

function setCredit(){
    $.post(
        CONTEXT_PATH+"/discuss/credit",
        {"id": $("#postId").val()},
        function (data){
            data = $.parseJSON(data);
            if (data.code == 0){
                $("#creditBtn").attr("disabled","disabled");
            } else{
                alert(data.msg);
            }
        }
    );
}
function setDelete(){
    $.post(
        CONTEXT_PATH+"/discuss/delete",
        {"id": $("#postId").val()},
        function (data){
            data = $.parseJSON(data);
            if (data.code == 0){
                location.href = CONTEXT_PATH+"/index";
            } else{
                alert(data.msg);
            }
        }
    );
}