
# Easy Image and Video Picker

This library is created to make image and video selection much easier




## Demo

![Alt Text](https://github.com/shahparshva/Image-Video-Picker/blob/master/ezgif.com-gif-maker.gif)

## Deployment

How to use this amazing lib and save your time 

```bash
   private val filePicker: MediaSelectHelper by lazy {
        MediaSelectHelper(this)
    }
```

```bash
    filePicker.registerCallback(object : MediaSelector {
            override fun onImageUri(uri: Uri) {
              //Do somthing on single image
            }

            override fun onImageUriList(uriArrayList: ArrayList<Uri>) {
               //Do somthing on single image list
            }

            override fun onVideoUri(uri: Uri) {
                //Do somthing on single video
            }

            override fun onVideoURIList(uriArrayList: java.util.ArrayList<Uri>) {
                 //Do somthing on single video list
            }
        })
```


## Select Image 
```bash
  //To change wather want single image selection or multiple 
  filePicker.canSelectMultiple(false)

  //If you want to show popup dilaog for selection for Gallery and Camera
  filePicker.showImageMenu(it,isCrop1=true,cropType = MediaSelectHelper.Constant.CropSquare).
  
  //else simple alert dialog 
  filePicker.selectOptionsForImagePicker(isCrop1=true,cropType = MediaSelectHelper.Constant.CropSquare)
```

There are currently 3 crop type used in this lib

CropSquare,CropRectangle,CropCircle

## Select Video 
```bash
  //To change wather want single video selection or multiple 
    filePicker.canSelectMultipleVideo(false)//set true if allow multiple
    filePicker.selectVideo()
```


