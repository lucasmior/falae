class CreatePages < ActiveRecord::Migration[5.1]
  def change
    create_table :pages do |t|
      t.string :name, null: false
      t.integer :columns
      t.integer :rows
      t.references :spreadsheet, foreign_key: true

      t.timestamps
    end
  end
end
