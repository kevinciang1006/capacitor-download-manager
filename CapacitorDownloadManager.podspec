
  Pod::Spec.new do |s|
    s.name = 'CapacitorDownloadManager'
    s.version = '0.0.1'
    s.summary = 'Capacitor Download Manager Plugin'
    s.license = 'MIT'
    s.homepage = 'github.com/kevinciang1006'
    s.author = 'Kevin Ciang'
    s.source = { :git => 'github.com/kevinciang1006', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
  end