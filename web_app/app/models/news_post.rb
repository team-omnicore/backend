class NewsPost < ActiveRecord::Base
  belongs_to :person

  validates_presence_of :person
  validates_presence_of :text

  named_scope :sort_by_creation, :order => "created_at DESC"
  named_scope :sort_by_update, :order => "updated_at DESC"
  named_scope :sort_by_publishing, :order => "published_at DESC"
  named_scope :published, :conditions => "published_at IS NOT NULL"
  
  def initialize(params = {})
    super(params)

    unless params.empty?
      unless params[:title] and self.title.empty?
       self.title = self.text.first(40)
       self.title << "..." if self.text.length > 40
       self.save!
     end
     translate!
    end
  end

  def translate!
    # TODO: implement BBCode or Markdown as input and translate to html!
    self.html = self.text
    self.save!
  end

  def published?
    published_at and published_at < Time.now
  end

  def publish!
    self.published_at = Time.now
    self.save!
  end

  def unpublish!
    self.published_at = nil
    self.save!
  end

  def was_updated?
    updated_at > (created_at + 1.hour)
  end

end