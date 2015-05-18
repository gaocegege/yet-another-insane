object Quicksorttest {
     def main(args: Array[String]): Unit = { 
      var arrays = Array(6,3,5,1,2,8,9,0,4,7);
      arrays=sort(arrays);
     }

      def sort(xs: Array[Int]):Array[Int] = {  
       if(xs.length <= 1)  
        xs;  
       else {  
         val pivot = xs(xs.length /2);  
       Array.concat(  
           sort(xs filter (pivot >)),  
                 xs filter (pivot ==),  
           sort(xs filter (pivot <))  
      )  
    } 
    } 
 }