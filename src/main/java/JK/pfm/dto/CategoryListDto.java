

package JK.pfm.dto;


public class CategoryListDto {
    private Long id;
    private String name;
    private boolean active;
    
    public CategoryListDto(){}
    
    public CategoryListDto(Long id, String name, boolean active){
        this.id = id;
        this.name = name;
        this.active = active;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
    
    public boolean getActive(){
        return active;
    }
    public void setActive(boolean active){
        this.active = active;
    }
}
